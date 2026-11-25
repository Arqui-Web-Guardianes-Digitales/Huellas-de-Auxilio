package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.dtos.MascotaDTO;
import com.upc.huellasdeauxilio.entidades.Entidad;
import com.upc.huellasdeauxilio.entidades.Mascota;
import com.upc.huellasdeauxilio.repositorios.EntidadRepositorio;
import com.upc.huellasdeauxilio.repositorios.MascotaRepositorio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.stream.Collectors;


@Service
public class MascotaServicio {
    @Autowired
    private MascotaRepositorio mascotaRepositorio;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EntidadRepositorio entidadRepositorio;

    @Autowired
    private Validator validator;

    public List<MascotaDTO> listarDisponibles() {
        return mascotaRepositorio.findByEstadoTrue().stream().
                map(mascota -> modelMapper.map(mascota, MascotaDTO.class)).
                collect(Collectors.toList());
    }

    public MascotaDTO buscarPorId(Long idMascota)
    {
        if (idMascota == null)
        {
            return null;
        }

        Mascota mascota = mascotaRepositorio.findById(idMascota).orElse(null);

        if (mascota == null)
        {
            return null;
        }

        MascotaDTO dto = modelMapper.map(mascota, MascotaDTO.class);

        if (mascota.getEntidad() != null) {dto.setEntidadIdEntidad(mascota.getEntidad().getIdEntidad()
        );
        }

        return dto;
    }

    //esta es una funcion extra para quitar especios extra y transformar
    //mayusculas a minusculas y facilitar la filtracion
    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return "todos";
        }

        return valor.trim().toLowerCase(Locale.ROOT);
    }

    public List<MascotaDTO> filtrarMascotas(String nombre, String especie,
                                            String edad, String distrito) {

        return mascotaRepositorio.filtrarMascotas(
                        normalizarFiltro(nombre),
                        normalizarFiltro(especie),
                        normalizarFiltro(edad),
                        normalizarFiltro(distrito)
                )
                .stream()
                .map(mascota -> {

                    MascotaDTO dto = modelMapper.map(
                            mascota,
                            MascotaDTO.class
                    );

                    if (mascota.getEntidad() != null) {
                        dto.setEntidadIdEntidad(
                                mascota.getEntidad().getIdEntidad()
                        );
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<MascotaDTO> listarDisponiblesPorEntidad(Long idEntidad) {

        return mascotaRepositorio
                .findByEntidadIdEntidadAndEstadoTrue(idEntidad)
                .stream()
                .map(mascota -> {

                    MascotaDTO dto = modelMapper.map(
                            mascota,
                            MascotaDTO.class
                    );

                    dto.setEntidadIdEntidad(
                            mascota.getEntidad().getIdEntidad()
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<MascotaDTO> filtrarMascotasPorEntidad(Long idEntidad,
                                                      String busqueda,
                                                      String especie,
                                                      String edad) {

        return mascotaRepositorio.filtrarMascotasPorEntidad(
                        idEntidad,
                        normalizarFiltro(busqueda),
                        normalizarFiltro(especie),
                        normalizarFiltro(edad)
                )
                .stream()
                .map(mascota -> {

                    MascotaDTO dto = modelMapper.map(
                            mascota,
                            MascotaDTO.class
                    );

                    dto.setEntidadIdEntidad(
                            mascota.getEntidad().getIdEntidad()
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public MascotaDTO publicarMascota(Long idEntidad, MascotaDTO mascotaDTO) {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida.");
        }
        if (idEntidad == null || idEntidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de entidad inválido.");
        }
        Entidad entidad = entidadRepositorio.findById(idEntidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidad no encontrada."));
        boolean esEntidad = autenticacion.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_ENTIDAD".equals(rol.getAuthority()));
        if (!esEntidad || entidad.getUsuario() == null
                || !autenticacion.getName().equals(entidad.getUsuario().getCorreo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes publicar para tu propia entidad.");
        }
        validarDatosMascota(mascotaDTO);

        // Convertir DTO a entidad
        Mascota mascota = modelMapper.map(mascotaDTO, Mascota.class);

        // Estos datos los establece el backend, no el cliente
        mascota.setIdMascota(null);
        mascota.setEntidad(entidad);
        mascota.setEstado(true);

        // Guardar en la base de datos
        Mascota mascotaGuardada = mascotaRepositorio.save(mascota);

        // Convertir la entidad guardada a DTO
        MascotaDTO respuesta = modelMapper.map(
                mascotaGuardada,
                MascotaDTO.class
        );

        respuesta.setEntidadIdEntidad(entidad.getIdEntidad());

        return respuesta;
    }

    private void validarDatosMascota(MascotaDTO mascotaDTO) {
        if (mascotaDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa los datos de la mascota.");
        }
        var errores = validator.validate(mascotaDTO);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
        if (!edadValida(mascotaDTO.getEdad())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La edad debe ser positiva en meses o años, o conservar su categoría anterior.");
        }
        mascotaDTO.setNombre(mascotaDTO.getNombre().trim());
        mascotaDTO.setDescripcion(mascotaDTO.getDescripcion().trim());
        mascotaDTO.setEdad(mascotaDTO.getEdad().trim());
        mascotaDTO.setUrlFoto(mascotaDTO.getUrlFoto().trim());

    }

    private boolean edadValida(String edad) {
        String valor = normalizarFiltro(edad);
        // Compatibilidad con clientes y registros anteriores, sin inventar una edad exacta.
        if (List.of("cachorro", "joven", "adulto").contains(valor)) return true;
        Matcher coincidencia = Pattern.compile("^([0-9]+(?:[.,][0-9]+)?(?:e[+-]?[0-9]+)?) (mes|meses|año|años)$")
                .matcher(valor);
        if (!coincidencia.matches()) return false;
        try {
            double cantidad = Double.parseDouble(coincidencia.group(1).replace(',', '.'));
            double meses = coincidencia.group(2).startsWith("año") ? cantidad * 12 : cantidad;
            return Double.isFinite(meses) && meses > 0;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    public MascotaDTO editarMascota(Long idMascota, MascotaDTO datosMascota) {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida.");
        }
        if (idMascota == null || idMascota <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de mascota inválido.");
        }
        Mascota mascota = mascotaRepositorio.findById(idMascota)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mascota no encontrada."));
        boolean esEntidad = autenticacion.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_ENTIDAD".equals(rol.getAuthority()));
        if (!esEntidad || mascota.getEntidad() == null || mascota.getEntidad().getUsuario() == null
                || !autenticacion.getName().equals(mascota.getEntidad().getUsuario().getCorreo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar mascotas de tu propia entidad.");
        }
        validarDatosMascota(datosMascota);

        // Actualizar únicamente los atributos editables
        mascota.setNombre(datosMascota.getNombre());
        mascota.setEspecie(datosMascota.getEspecie());
        mascota.setEdad(datosMascota.getEdad());
        mascota.setSexo(datosMascota.getSexo());
        mascota.setDistrito(datosMascota.getDistrito());
        mascota.setTamaño(datosMascota.getTamaño());
        mascota.setDescripcion(datosMascota.getDescripcion());
        mascota.setUrlFoto(datosMascota.getUrlFoto());

        // Guardar los cambios
        Mascota mascotaActualizada = mascotaRepositorio.save(mascota);

        // Convertir la entidad actualizada a DTO
        MascotaDTO respuesta = modelMapper.map(
                mascotaActualizada,
                MascotaDTO.class
        );

        respuesta.setEntidadIdEntidad(
                mascotaActualizada.getEntidad().getIdEntidad()
        );

        return respuesta;
    }

    public Mascota cambiarEstado(Long idMascota, Boolean estado) {

        if (idMascota == null) {
            return null;
        }

        Mascota mascota = mascotaRepositorio
                .findById(idMascota)
                .orElse(null);

        if (mascota == null || estado == null) {
            return null;
        }

        mascota.setEstado(estado);

        return mascotaRepositorio.save(mascota);
    }



}
