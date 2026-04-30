# 🚗 AutoCare App

Aplicación móvil desarrollada en Android Studio utilizando **Jetpack Compose**, diseñada para gestionar el mantenimiento de vehículos de forma sencilla, visual y organizada.

---

## 📱 Descripción

AutoCare es una aplicación que permite a los usuarios llevar un control de los servicios realizados a su vehículo, incluyendo costos, fechas y evidencias (fotos). Además, incluye un sistema básico de autenticación y gestión de perfil.

---

## ⚙️ Funcionalidades principales

- Registro e inicio de sesión de usuarios  
- Gestión de perfil (nombre, correo, contraseña, foto)  
- Registro de servicios del vehículo  
- Edición y eliminación de servicios  
- Visualización de historial de mantenimiento  
- Subida de imágenes (vehículo o recibos)  
- Selector de fecha para los servicios  
- Cambio entre modo claro y oscuro  

---

## 🧩 Estructura de pantallas

La aplicación está compuesta por varias vistas principales:

- **Login**
  - Inicio de sesión con validación básica  

- **Registro**
  - Creación de cuenta con validaciones  

- **Inicio (Home)**
  - Vista principal con historial de servicios  

- **Formulario de Servicio**
  - Crear o editar registros de mantenimiento  

- **Perfil**
  - Edición de datos del usuario  

---

## 🛠️ Tecnologías utilizadas

- Kotlin  
- Jetpack Compose  
- Navigation Compose  
- Material Design 3  
- ViewModel (MVVM)  
- Activity Result API (selección de imágenes)  
- Coil (carga de imágenes con AsyncImage)  

---

## 🧠 Arquitectura

Se utiliza el patrón **MVVM (Model - View - ViewModel)**:

- **View**: Composables (interfaz de usuario)  
- **ViewModel**: Manejo del estado y lógica  
- **Model**: Datos del usuario y servicios  

---

## 📂 Funcionalidades destacadas

### 🔐 Autenticación

- Validación de correo (debe contener "@")  
- Manejo de sesión básica  
- Navegación entre pantallas  

### 📸 Manejo de imágenes

- Selección desde galería  
- Uso de imágenes para:
  - Perfil  
  - Vehículo  
  - Recibos de servicios  

### 📊 Gestión de servicios

Cada servicio incluye:

- Tipo de servicio  
- Fecha  
- Costo  
- Imagen opcional  

---

## ▶️ Cómo ejecutar el proyecto

1. Clonar el repositorio:

```bash
git clone <url-del-repositorio>
