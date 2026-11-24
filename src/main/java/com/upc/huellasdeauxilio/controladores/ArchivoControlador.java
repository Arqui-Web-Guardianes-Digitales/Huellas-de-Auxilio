package com.upc.huellasdeauxilio.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ArchivoControlador {

    // El nombre de la carpeta en la raíz del proyecto, osea al lado de pom.xml
    private final String CARPETA_IMAGENES = "imagenes";

    @PostMapping(value = "/archivos/subir", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public ResponseEntity<Map<String, String>> subirImagen(@RequestParam("archivo") MultipartFile archivo) {
        Map<String, String> respuesta = new HashMap<>();

        try {
            validarImagen(archivo);
            // 1 crear la carpeta "imagenes" si no existe en el disco duro, solo si no existe
            File directorio = new File(CARPETA_IMAGENES);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // 2 generar un nombre único para que no se chanquen las fotos
            // ejemplo: foto.jpg -> 1234abcd-foto.jpg
            String nombreOriginal = archivo.getOriginalFilename().replace('\\', '/');
            String nombreSeguro = nombreOriginal.substring(nombreOriginal.lastIndexOf('/') + 1)
                    .replaceAll("[^\\p{L}\\p{N}._ -]", "_");
            if (nombreSeguro.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de imagen inválido.");
            String nombreUnico = UUID.randomUUID().toString() + "-" + nombreSeguro;

            // 3 ruta completa donde se guardará
            Path rutaFisica = Paths.get(CARPETA_IMAGENES).resolve(nombreUnico).toAbsolutePath();

            // 4 copiar el archivo que mandó el usuario a nuestra carpeta
            Files.copy(archivo.getInputStream(), rutaFisica);

            // 5 generar la URL con la que Angular podrá ver la imagen
            String urlImagen = "http://localhost:8088/imagenes/" + nombreUnico;

            // 6 devolver la URL
            respuesta.put("url", urlImagen);
            return new ResponseEntity<>(respuesta, HttpStatus.OK);

        } catch (IOException e) {
            respuesta.put("error", "Ocurrió un error al subir la imagen");
            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validarImagen(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen no vacía.");
        }
        // Mismo máximo de archivo que utiliza Spring por defecto y el formulario de mascotas.
        if (archivo.getSize() > 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.valueOf(413), "La imagen admite hasta 1 MB.");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe tener un nombre.");
        }
        byte[] contenido = archivo.getBytes();
        String tipo = "";
        if (contenido.length >= 8 && contenido[0] == (byte) 137 && contenido[1] == 80
                && contenido[2] == 78 && contenido[3] == 71) tipo = "image/png";
        else if (contenido.length >= 3 && contenido[0] == (byte) 255 && contenido[1] == (byte) 216
                && contenido[2] == (byte) 255) tipo = "image/jpeg";
        else if (webpValido(contenido)) tipo = "image/webp";
        // Mantiene también GIF y BMP para los otros módulos que usan accept="image/*".
        else if (contenido.length >= 6 && new String(contenido, 0, 6, java.nio.charset.StandardCharsets.US_ASCII).matches("GIF8[79]a")) tipo = "image/gif";
        else if (contenido.length >= 2 && contenido[0] == 'B' && contenido[1] == 'M') tipo = "image/bmp";
        boolean decodificable = false;
        try {
            decodificable = tipo.equals("image/webp") || ImageIO.read(new ByteArrayInputStream(contenido)) != null;
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen está dañada o incompleta.");
        }
        if (tipo.isEmpty() || !decodificable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no contiene una imagen PNG, JPG, WebP, GIF o BMP válida.");
        }
        String mime = archivo.getContentType();
        if (mime != null && !mime.equals("application/octet-stream") && !mime.equalsIgnoreCase(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo del archivo no coincide con su contenido.");
        }
        String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        boolean extensionValida = switch (tipo) {
            case "image/png" -> extension.equals("png");
            case "image/jpeg" -> List.of("jpg", "jpeg", "jpe").contains(extension);
            case "image/webp" -> extension.equals("webp");
            case "image/gif" -> extension.equals("gif");
            default -> extension.equals("bmp");
        };
        if (!extensionValida) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La extensión de la imagen no coincide con su contenido.");
    }

    private boolean webpValido(byte[] datos) {
        if (datos.length < 20 || !texto(datos, 0, "RIFF") || !texto(datos, 8, "WEBP")) return false;
        ByteBuffer buffer = ByteBuffer.wrap(datos).order(ByteOrder.LITTLE_ENDIAN);
        if (Integer.toUnsignedLong(buffer.getInt(4)) + 8 != datos.length) return false;
        boolean imagen = false;
        int posicion = 12;
        while (posicion + 8 <= datos.length) {
            long longitud = Integer.toUnsignedLong(buffer.getInt(posicion + 4));
            long fin = posicion + 8L + longitud;
            if (fin > datos.length) return false;
            if (texto(datos, posicion, "VP8 ")) {
                if (longitud < 10 || datos[posicion + 11] != (byte) 157
                        || datos[posicion + 12] != 1 || datos[posicion + 13] != 42) return false;
                imagen = true;
            } else if (texto(datos, posicion, "VP8L")) {
                if (longitud < 5 || datos[posicion + 8] != 47) return false;
                imagen = true;
            } else if (texto(datos, posicion, "ANMF")) {
                if (longitud < 24) return false;
                imagen = true;
            }
            posicion = (int) (fin + longitud % 2);
        }
        return imagen && posicion == datos.length;
    }

    private boolean texto(byte[] datos, int inicio, String texto) {
        return new String(datos, inicio, 4, java.nio.charset.StandardCharsets.US_ASCII).equals(texto);
    }

}
