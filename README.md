# AutoCare App

Aplicación móvil desarrollada en Android Studio con Kotlin y Jetpack Compose. Su objetivo es ayudar al usuario a registrar y organizar los servicios de mantenimiento de su vehículo.

## Descripción

AutoCare permite llevar un control básico del mantenimiento automotriz, registrando servicios realizados, fechas, costos e imágenes relacionadas. La app también cuenta con pantallas de inicio de sesión, registro, perfil y gestión de servicios.

## Funcionalidades

- Inicio de sesión y registro de usuario.
- Validación básica de datos.
- Gestión de perfil del usuario.
- Registro de servicios del vehículo.
- Edición y eliminación de servicios.
- Visualización del historial de mantenimiento.
- Selección de imágenes desde la galería.
- Cambio entre modo claro y modo oscuro.

## Pantallas principales

- LoginScreen: permite iniciar sesión.
- RegisterScreen: permite crear una cuenta nueva.
- HomeScreen: muestra el historial de servicios registrados.
- ServiceFormScreen: permite agregar o editar un servicio.
- ProfileScreen: permite editar la información del usuario.

## Tecnologías utilizadas

- Kotlin
- Android Studio
- Jetpack Compose
- Material Design 3
- Navigation Compose
- ViewModel
- Coil

## Arquitectura

La aplicación utiliza una estructura basada en MVVM:

- Model: representa los datos del usuario y los servicios.
- View: contiene las pantallas creadas con Jetpack Compose.
- ViewModel: administra el estado y la lógica de la aplicación.

## Datos que maneja la app

Cada servicio registrado puede contener:

- Nombre o tipo de servicio.
- Fecha del servicio.
- Costo.
- Imagen opcional como evidencia.

## Cómo ejecutar el proyecto

1. Clonar el repositorio:

```bash
git clone <url-del-repositorio>
