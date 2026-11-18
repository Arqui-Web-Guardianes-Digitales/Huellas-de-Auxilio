--ROLES
INSERT INTO rol (tipo_rol) VALUES ('ROLE_CIUDADANO');
INSERT INTO rol (tipo_rol) VALUES ('ROLE_ENTIDAD');

--USUARIOS, todas las contraseñas son: 123456
INSERT INTO usuario (correo, telefono, contraseña, estado_usuario, id_rol) VALUES ('juan@gmail.com', '999999999', '$2a$12$1k34YdrmxBkVborQvZLh2OUvX1S80GVVQjZJ5H55y1eez7XV.nV06', true, 1);
INSERT INTO usuario (correo, telefono, contraseña, estado_usuario, id_rol) VALUES ('maria@gmail.com', '988888888', '$2a$12$1k34YdrmxBkVborQvZLh2OUvX1S80GVVQjZJ5H55y1eez7XV.nV06', true, 1);
INSERT INTO usuario (correo, telefono, contraseña, estado_usuario, id_rol) VALUES ('refugiopatitas@gmail.com', '977777777', '$2a$12$1k34YdrmxBkVborQvZLh2OUvX1S80GVVQjZJ5H55y1eez7XV.nV06', true, 2);
INSERT INTO usuario (correo, telefono, contraseña, estado_usuario, id_rol) VALUES ('veterinaria@gmail.com', '966666666', '$2a$12$1k34YdrmxBkVborQvZLh2OUvX1S80GVVQjZJ5H55y1eez7XV.nV06', true, 2);

--PERFILES, ciudadanos y entidades
INSERT INTO ciudadano (dni, distrito, nombre_completo, id_usuario) VALUES ('12345678', 'San Miguel', 'Juan Perez', 1);
INSERT INTO ciudadano (dni, distrito, nombre_completo, id_usuario) VALUES ('87654321', 'Miraflores', 'Maria Lopez', 2);

INSERT INTO entidad (nombre_entidad, tipo_entidad, zona_atencion, id_usuario, latitud, longitud) VALUES ('Refugio Patitas', 'Refugio', 'San Miguel', 3, -12.0765, -77.0943);
INSERT INTO entidad (nombre_entidad, tipo_entidad, zona_atencion, id_usuario, latitud, longitud) VALUES ('Veterinaria Central', 'Veterinaria', 'Miraflores', 4, -12.1220, -77.0310);

--UBICACIONES DE REPORTES
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. La Marina 2000', 'San Miguel', 'Frente al parque', -12.0765, -77.0943);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Ovalo Gutierrez', 'Miraflores', 'Cerca al cine', -12.1100, -77.0350);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Parque Kennedy', 'Miraflores', 'En el centro', -12.1215, -77.0295);

--REPORTES DE MALTRATO/ABANDONO
-- Juan reporta al Refugio Patitas (Estado: Recibido)
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (1, 1, 1, '2026-10-15', 'M', 'AL', 'Perrito amarrado bajo el sol sin agua todo el dia', 'R');
-- Maria reporta a Veterinaria Central (Estado: En Revisión)
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (2, 2, 2, '2026-10-18', 'A', 'ME', 'Gatito abandonado en caja de carton', 'ER');
-- Juan reporta a Veterinaria Central (Estado: Atendido)
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (3, 1, 2, '2026-10-20', 'A', 'BA', 'Paloma con ala rota en el parque', 'AT');

--MASCOTAS EN ADOPCIÓN
--Refugio Patitas publica
INSERT INTO mascota (id_entidad, nombre, edad, sexo, distrito, tamaño, estado, especie, url_foto, descripcion) VALUES (1, 'Max', 'Adulto', 'Macho', 'San Miguel', 'Grande', false, 'Perro', 'http://localhost:8088/imagenes/251de6de-7119-4776-8be6-405ba969e368-max-perro.png', 'Cariñoso, jugueton y muy protector.');
INSERT INTO mascota (id_entidad, nombre, edad, sexo, distrito, tamaño, estado, especie, url_foto, descripcion) VALUES (1, 'Luna', 'Cachorro', 'Hembra', 'San Miguel', 'Pequeño', true, 'Gato', 'http://localhost:8088/imagenes/b296be2d-0ab9-4e51-85dc-0fda15b8375f-luna-gato.png', 'Muy tranquila, ideal para departamentos.');
--Veterinaria Central publica
INSERT INTO mascota (id_entidad, nombre, edad, sexo, distrito, tamaño, estado, especie, url_foto, descripcion) VALUES (2, 'Rocky', 'Joven', 'Macho', 'Miraflores', 'Mediano', true, 'Perro', 'http://localhost:8088/imagenes/ccfa5f19-115e-44e4-a414-82b7cfb3f189-rocky-perro.png', 'Lleno de energia, necesita espacio para correr.');

--SOLICITUDES DE ADOPCIÓN
--Juan quiere adoptar a Luna (En revisión)
INSERT INTO solicitud (id_mascota, id_ciudadano, fecha_solicitud, tipo_vivienda, motivo, experiencia, estado_solicitud) VALUES (2, 1, '2026-10-25 10:30:00', 'Departamento', 'Busco compania', true, 'EN_REVISION');
--Maria quiere adoptar a Max (Aprobada)
INSERT INTO solicitud (id_mascota, id_ciudadano, fecha_solicitud, tipo_vivienda, motivo, experiencia, estado_solicitud) VALUES (1, 2, '2026-10-26 14:15:00', 'Casa', 'Tenemos espacio', true, 'APROBADA');
--Juan queria adoptar a Max pero fue Rechazado (porque Maria lo adoptó)
INSERT INTO solicitud (id_mascota, id_ciudadano, fecha_solicitud, tipo_vivienda, motivo, experiencia, estado_solicitud) VALUES (1, 1, '2026-10-26 09:00:00', 'Departamento', 'Me gusta la raza', false, 'RECHAZADA');

-- EVIDENCIAS DE REPORTES (Fotos de prueba para Juan y María)
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (1, 'http://localhost:8088/imagenes/4020fe65-fbb0-4931-a4a8-af88e90c625b-perrito1.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (2, 'http://localhost:8088/imagenes/c78608e3-b126-47fc-8604-73d81f64ed78-gato perdido.png');
-- UBICACIONES ADICIONALES PARA PRUEBAS (IDs 4 a 13)
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. La Marina 2000', 'San Miguel', 'Frente al parque', -12.076500, -77.094300);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. La Marina', 'San Miguel', 'Cerca de la cuadra 20', -12.077000, -77.094000);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Simón Bolívar 1515', 'Pueblo Libre', 'Frente al Museo Larco', -12.072503, -77.070841);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Simón Bolívar', 'Pueblo Libre', 'Cerca del ingreso al Museo Larco', -12.072600, -77.070800);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Horacio Urteaga', 'Jesús María', 'Parque San José', -12.074310, -77.048130);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Gregorio Escobedo 781', 'Jesús María', 'Cerca del Teatro Peruano Japonés', -12.087500, -77.055300);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Diagonal', 'Miraflores', 'Parque Kennedy', -12.121500, -77.029500);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Óvalo Gutiérrez', 'Miraflores', 'Cerca del cine', -12.110000, -77.035000);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Caminos del Inca', 'Surco', 'Parque de la Amistad', -12.131730, -76.981750);
INSERT INTO ubicacion (direccion, distrito, referencia, latitud, longitud) VALUES ('Av. Juan Pardo de Zela 480', 'Lince', 'Frente a la municipalidad', -12.083344, -77.031796);

-- REPORTES ADICIONALES PARA PRUEBAS (IDs 4 a 13)
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (4, 1, 1, '2026-11-01', 'M', 'AL', 'Perro atado al sol sin agua.', 'R');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (5, 2, 1, '2026-11-02', 'A', 'ME', 'Gatito solo en una caja junto a la vereda.', 'ER');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (6, 1, 1, '2026-11-03', 'A', 'BA', 'Perro sin collar que deambulaba cerca del museo.', 'AT');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (7, 2, 1, '2026-11-04', 'M', 'ME', 'Se reporta que un perro permanecía atado con una correa muy corta.', 'R');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (8, 1, 1, '2026-11-05', 'A', 'AL', 'Cachorro abandonado junto al parque, cerca de la calzada.', 'ER');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (9, 2, 1, '2026-11-06', 'M', 'BA', 'Se reporta trato brusco hacia un perro en la vía pública.', 'AT');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (10, 2, 2, '2026-11-01', 'A', 'AL', 'Gato abandonado en una caja cerca de la avenida.', 'R');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (11, 1, 2, '2026-11-03', 'M', 'ME', 'Se reporta que un perro permanecía atado sin acceso a agua.', 'ER');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (12, 2, 2, '2026-11-05', 'A', 'BA', 'Perro sin collar encontrado en los alrededores del parque.', 'AT');
INSERT INTO reporte (id_ubicacion, id_ciudadano, id_entidad, fecha_reporte, tipo_caso, nivel_urgencia, descripcion, estado) VALUES (13, 1, 2, '2026-11-06', 'M', 'AL', 'Se reporta a un perro expuesto al sol, sin agua y con signos de agotamiento.', 'ER');

-- EVIDENCIAS ADICIONALES (HA-013 queda sin evidencia)
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (4, 'http://localhost:8088/imagenes/1e36632f-6593-444b-ac16-2b642a60bda8-evidencia-foto-1.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (5, 'http://localhost:8088/imagenes/09b2a907-1892-4caf-a206-d09e57903d69-evidencia-foto-2.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (6, 'http://localhost:8088/imagenes/b3b732f4-04a3-41c3-908d-260411757753-evidencia2.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (7, 'http://localhost:8088/imagenes/1e36632f-6593-444b-ac16-2b642a60bda8-evidencia-foto-1.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (8, 'http://localhost:8088/imagenes/09b2a907-1892-4caf-a206-d09e57903d69-evidencia-foto-2.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (9, 'http://localhost:8088/imagenes/b3b732f4-04a3-41c3-908d-260411757753-evidencia2.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (10, 'http://localhost:8088/imagenes/1e36632f-6593-444b-ac16-2b642a60bda8-evidencia-foto-1.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (11, 'http://localhost:8088/imagenes/09b2a907-1892-4caf-a206-d09e57903d69-evidencia-foto-2.png');
INSERT INTO evidencia_reporte (id_reporte, url_evidencia) VALUES (12, 'http://localhost:8088/imagenes/b3b732f4-04a3-41c3-908d-260411757753-evidencia2.png');
