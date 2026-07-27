# 💰 FinFlow — Simulador de Presupuesto (Versión JSF / Jakarta EE)

![Java](https://img.shields.io/badge/Java-11%2B-orange?style=for-the-badge&logo=java)
![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-JSF-red?style=for-the-badge&logo=jakartaee)
![Open Liberty](https://img.shields.io/badge/Server-Open%20Liberty-blue?style=for-the-badge)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Supported-2496ED?style=for-the-badge&logo=docker)

**FinFlow** es un simulador de presupuesto personal y empresarial desarrollado con la plataforma **Jakarta EE / JSF (JavaServer Faces)** y desplegado sobre el servidor de aplicaciones **Open Liberty**. 

El sistema reemplaza las arquitecturas convencionales tipo Spring Boot integrando la especificación **CDI**, **JSF (Facelets)** y **JPA/Hibernate** para la gestión financiera, autenticación por roles, simulación presupuestaria y un módulo interactivo ludificado (*Arcade*).

---

## 🚀 Características Principales

* 📊 **Gestión Financiera Completa:**
  * Control de **Ingresos** y **Gastos** detallados.
  * Asignación de categorías y **Niveles de Prioridad** (*Alta, Media, Baja*).
  * Monitoreo y cálculo automático de saldos y presupuestos disponibles.
* 🕹️ **Módulo Arcade / Gamificación:**
  * Integración de mecánicas de juego vinculadas a la cuenta del usuario (`CuentaJuego`).
  * Tabla de clasificación y recompensas de puntuación.
* 🔒 **Seguridad y Control de Acceso:**
  * Filtro de seguridad personalizado (`SeguridadFilter`) para la protección de URLs.
  * Control de acceso basado en roles (**Administrador** y **Usuario**).
  * Panel de control exclusivo para administración del sistema (`/admin/panel.xhtml`).
* 📧 **Servicios y Utilidades:**
  * Servicio integrado de correo electrónico (`EmailService`) para la recuperación de contraseña.
  * Inicializador de datos por defecto para el usuario administrador (`AdminInicializadorListener`).

---

## 🛠️ Arquitectura y Tecnologías

* **Lenguaje:** Java 11+
* **Tecnología Web:** JSF (JavaServer Faces) + Facelets
* **Contenedor CDI & Servidor:** Open Liberty
* **Persistencia:** JPA / Hibernate (`persistence.xml`)
* **Base de Datos:** PostgreSQL
* **Construcción y Dependencias:** Apache Maven (`pom.xml`)
* **Orquestación:** Docker & Docker Compose (`docker-compose.yml`)

---

## 📁 Estructura del Proyecto

```text
SimuladorPresupuestoJSF/
├── src/
│   └── main/
│       ├── java/edu/unl/cc/
│       │   ├── dominio/        # Entidades JPA (Usuario, Presupuesto, Gasto, Ingreso, etc.)
│       │   ├── repositorio/    # Repositorios JPA y EntityManagerProducer
│       │   └── web/            # ManagedBeans (AuthBean, MovimientoBean, ArcadeBean, etc.)
│       ├── liberty/config/     # Configuración del servidor Open Liberty (server.xml)
│       ├── resources/META-INF/ # Configuración JPA (persistence.xml)
│       └── webapp/             # Vistas Facelets (.xhtml), Plantillas y CSS
├── docker-compose.yml          # Despliegue del contenedor de PostgreSQL y la aplicación
└── pom.xml                     # Configuración del proyecto Maven
