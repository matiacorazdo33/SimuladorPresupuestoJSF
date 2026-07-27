# FinFlow — Simulador de Presupuesto (versión JSF / Jakarta EE)

Versión **JSF (Jakarta Server Faces)** del Simulador de Presupuesto,
desplegada en **Open Liberty** — reemplaza a la versión Spring Boot
(`SimuladorPresupuestoWeb`) manteniendo la misma lógica de negocio y el
mismo diseño visual ("FinFlow"), pero con la arquitectura propia de
Jakarta EE: beans gestionados por **CDI** en vez de controladores Spring,
páginas **Facelets (.xhtml)** en vez de Thymeleaf, y **JPA/Hibernate**
sobre PostgreSQL con `persistence.xml` en vez de Spring Data.

## Equivalencias entre versiones

| Spring Boot | JSF / Jakarta EE |
|---|---|
| `@Controller` | `@Named` + `@RequestScoped`/`@SessionScoped` (CDI managed bean) |
| Thymeleaf (`.html`) | Facelets (`.xhtml`) |
| `application.properties` (datasource) | `META-INF/persistence.xml` |
| Spring Data JPA (`UsuarioJpaRepository`) | `EntityManager` inyectado por CDI, consultas JPQL manuales |
| `HttpSession` + interceptores | `AuthBean` (`@SessionScoped`) + `SeguridadFilter` (`@WebFilter`) |
| `CommandLineRunner` (admin inicial) | `ServletContextListener` (`@WebListener`) |
| Tomcat embebido | Open Liberty (servidor Jakarta EE completo) |

Las **entidades de dominio** (`Usuario`, `Cuenta`, `Presupuesto`,
`Transaccion`, `Ingreso`, `Gasto`, `Categoria`, `Prioridad`, `Rol`) se
reutilizaron **sin cambios de lógica** — ya eran JPA puro en la versión
Spring, así que se portaron literalmente.

## Funcionalidad incluida (paridad completa con la versión Spring)

- Registro de cuenta y login con bloqueo tras 3 intentos fallidos
- **Correo de recuperación de contraseña** al bloquearse el acceso (con
  respaldo por consola si no se configura SMTP)
- Dashboard con balance, XP, progreso del presupuesto y accesos rápidos
- Registrar ingresos y gastos (rechazando gastos que superan el saldo)
- Historial con **calendario mensual** (días en verde/rojo/naranja según
  ingresos/gastos/ambos)
- Página de presupuesto con barra de progreso
- Configuración de saldo y límite mensual desde el propio dashboard
- **Modo emergencia**: toda la interfaz cambia a tonos rojos cuando se
  supera el límite mensual
- Avatar flotante con menú desplegable (saldo, accesos rápidos, cerrar sesión)
- Panel de administración: dashboard con estadísticas globales, lista de
  usuarios, edición de saldo/límite de cualquier usuario, **eliminación de
  movimientos individuales** (ajustando el saldo automáticamente) y
  eliminación de usuarios

## Cómo ejecutarlo

Requisitos: **Java 21**, **Maven**, **Docker Desktop**.

### 1. Levantar la base de datos

```bash
docker compose up -d
```

Crea dos contenedores:
- `simulador-presupuesto-jsf-db` — PostgreSQL, con la base
  `simulador_presupuesto` / usuario `simulador` / contraseña `simulador`
  (si ya tienes el contenedor de la versión Spring corriendo de antes, este
  usa un nombre y volumen distintos para no chocar)
- `simulador-presupuesto-jsf-adminer` — **Adminer**, una interfaz web para
  ver y editar la base de datos. Ver la sección
  ["Adminer"](#adminer-interfaz-web-para-la-base-de-datos) más abajo para
  los detalles de acceso.

### 2. Compilar y arrancar el servidor Liberty

```bash
mvn liberty:run
```

La primera vez descargará el runtime de Open Liberty además de las
dependencias Maven — puede tardar unos minutos. Cuando veas
`[INFO] [AUDIT] CWWKF0011I: The server ... is ready to run` la app está
lista en:

```
http://localhost:9080
```

Para desarrollo con recarga en caliente de `.xhtml`, usa `mvn liberty:dev`
en su lugar.

## Cuenta de administrador

Se crea automáticamente al arrancar (ver consola):

- **Usuario/correo:** `admin` / `admin@simulador.com`
- **Contraseña:** `admin123`

Cámbialas editando las constantes en
`src/main/java/edu/unl/cc/web/AdminInicializadorListener.java`.

## Adminer (interfaz web para la base de datos)

[Adminer](https://www.adminer.org/) es una herramienta liviana para
ver, consultar y editar la base de datos PostgreSQL directamente desde el
navegador, sin instalar ningún cliente de escritorio (como DBeaver o
pgAdmin). Ya viene incluida en `docker-compose.yml` y se levanta
automáticamente junto con la base de datos.

### Cómo entrar

1. Asegúrate de que los contenedores estén corriendo:
   ```bash
   docker compose up -d
   ```
2. Verifica que ambos aparezcan activos:
   ```bash
   docker ps
   ```
   Deberías ver `simulador-presupuesto-jsf-db` y
   `simulador-presupuesto-jsf-adminer`.
3. Abre el navegador en:
   ```
   http://localhost:8081
   ```
4. Llena el formulario de login de Adminer con estos datos:

   | Campo | Valor |
   |---|---|
   | Sistema | **PostgreSQL** |
   | Servidor | **db** |
   | Usuario | **simulador** |
   | Contraseña | **simulador** |
   | Base de datos | **simulador_presupuesto** |

### Qué puedes hacer ahí

- Ver la estructura de cada tabla (`usuarios`, `cuentas`, `presupuestos`,
  `transacciones`) — útil para confirmar que Hibernate creó/actualizó las
  columnas como se espera después de un cambio en las entidades.
- Consultar y filtrar registros (pestaña "Seleccionar datos" de cada
  tabla).
- Editar o borrar filas a mano, por ejemplo para limpiar datos de prueba
  sin tener que borrar todo el volumen de Docker.
- Ejecutar SQL directo desde "Comando SQL" en el menú lateral, para
  consultas puntuales de depuración.

### Notas

- Adminer es **independiente del servidor Liberty**: no hace falta tener
  la aplicación (`mvn liberty:run`) corriendo para usarlo, solo el
  contenedor de Docker.
- Es una herramienta de desarrollo/depuración, no algo que forme parte de
  la aplicación en sí — no se despliega junto con el `.war`.
- Si prefieres no tenerlo corriendo, puedes quitar el servicio `adminer`
  de `docker-compose.yml` sin afectar en nada al resto del proyecto.

## Correo de recuperación

Al igual que en la versión Spring, si no configuras credenciales SMTP la
app no falla: el enlace de recuperación se imprime en la consola del
servidor. Para enviar correos reales de verdad, edita las constantes al
inicio de `src/main/java/edu/unl/cc/web/EmailService.java`
(`SMTP_USERNAME`, `SMTP_PASSWORD`). Con Gmail necesitas una "contraseña
de aplicación" (no tu contraseña normal): actívala en
https://myaccount.google.com/apppasswords tras habilitar la verificación
en 2 pasos.

## Decisiones técnicas notables

- **Persistencia `RESOURCE_LOCAL`**: la app abre su propia conexión JDBC
  directamente vía `EntityManagerFactory`, sin necesitar configurar un
  `DataSource` JNDI en `server.xml`. Así el proyecto corre con solo Docker
  + Maven, sin tocar la configuración de Liberty.
- **`UsuarioRepositorio` es `@Dependent`** (no `@RequestScoped`): así puede
  usarse tanto en peticiones HTTP normales como en el arranque de la
  aplicación (`AdminInicializadorListener`), momento en el que todavía no
  existe ningún contexto de petición activo.
- **El productor del `EntityManager` está en su propia clase
  (`EntityManagerProducer`), separado de `UsuarioRepositorio`**: fusionarlos
  en un solo archivo (como se intentó en un momento) rompe el despliegue,
  porque `UsuarioRepositorio` terminaría inyectando un `EntityManager` que
  ella misma produce — Weld (el motor de CDI) no puede resolver esa
  dependencia circular en un bean `@Dependent` (no hay proxy de por medio
  que rompa el ciclo). El error es `WELD-001443: Pseudo scoped bean has
  circular dependencies`.
- **Los beans `@ViewScoped`/`@SessionScoped` no inyectan `UsuarioRepositorio`
  como campo** (`AuthBean`, `ArcadeJuegoBean`): esos ámbitos exigen que toda
  su cadena de dependencias sea serializable para poder "pasivarse", y un
  bean `@Dependent` con un `EntityManager` (tampoco formalmente
  serializable) no lo garantiza. Inyectarlo como campo produce
  `WELD-001413: UnserializableDependencyException`. En su lugar, piden una
  instancia fresca en cada llamada vía `CDI.current().select(...)`.
- **`AuthBean` no inyecta `UsuarioRepositorio` como campo**: al ser un
  bean de sesión (`@SessionScoped`) inyectar un `@Dependent` como campo lo
  "pegaría" a toda la sesión junto con su `EntityManager`, arriesgando
  lecturas obsoletas (caché de primer nivel de Hibernate) entre distintas
  peticiones. En su lugar pide una instancia fresca en cada llamada.
- **Parámetros de URL vía `f:viewParam`**: las páginas que dependen de un
  id o token en la URL (`recuperar.xhtml`, `admin/panel.xhtml`) usan el
  patrón estándar de JSF (`f:viewParam` + campos ocultos en los
  formularios) en vez de `@PathVariable` al estilo Spring, ya que JSF
  reconstruye los managed beans en cada postback.
- **Páginas fusionadas con un parámetro de URL**: en vez de un archivo
  `.xhtml` por cada variante casi idéntica de una pantalla, varias
  páginas se combinaron en una sola que cambia de contenido según un
  parámetro (ver tabla en "Páginas consolidadas" más abajo). Reduce el
  número de archivos sin duplicar HTML.

## Beans y páginas consolidadas

Para no tener una clase y un archivo `.xhtml` por cada pantalla pequeña,
varios beans y páginas casi idénticos se fusionaron en uno solo que
cambia de comportamiento según un parámetro de URL:

| Bean / página únicos | Antes eran... | Parámetro |
|---|---|---|
| `AuthBean` | `AuthBean` + `LogoutBean` + `EstadoCuentaBean` | — |
| `CuentaBean` | `MenuBean` + `PresupuestoBean` + `CuentaConfigBean` | — |
| `MovimientoBean` | `IngresoBean` + `GastoBean` | — |
| `AccesoBean` | `RegistroBean` + `LoginBean` | — |
| `AdminBean` | `AdminDashboardBean` + `AdminUsuariosBean` + `AdminUsuarioDetalleBean` | — |
| `HistorialBean.CalendarDia` | paquete `web/util/CalendarDia.java` aparte | — |
| `acceso.xhtml` | `registro.xhtml` + `login.xhtml` | `?modo=registro\|login` |
| `estado.xhtml` | `bloqueado.xhtml` + `error.xhtml` | `?tipo=bloqueado\|error` |
| `movimientoForm.xhtml` | `ingresoForm.xhtml` + `gastoForm.xhtml` | `?tipo=ingreso\|gasto` |
| `cuenta/configForm.xhtml` | `cuenta/saldoForm.xhtml` + `cuenta/limiteForm.xhtml` | `?campo=saldo\|limite` |
| `admin/panel.xhtml` | `admin/dashboard.xhtml` + `admin/usuarios.xhtml` + `admin/usuarioDetalle.xhtml` | `?vista=dashboard\|usuarios\|detalle` |

Resultado: **10 clases** en `web/` (antes 18), **1 clase** en
`repositorio/` (antes 2), y **13 páginas `.xhtml`** (antes 19). Las 9
entidades de `dominio/` no se tocaron: son clases JPA con mapeos
distintos (incluida la herencia `Transaccion`/`Ingreso`/`Gasto`), así que
fusionarlas rompería el modelo de datos.

## Zona Arcade

Implementada de forma **unificada y basada en datos**, en vez de una clase
por minijuego: los 7 minijuegos de preguntas (Quiz Financiero, Adivina el
Gasto, Reto de Ahorro, Simulador de Decisiones, Retos Financieros, Ciudad
del Ahorro y Laberinto de Deudas) comparten la misma mecánica —una serie
de rondas de opción múltiple— y solo cambia el banco de preguntas de cada
uno. Esto evita tener 8 clases casi idénticas y sigue el mismo criterio de
consolidación que el resto del proyecto.

- **`ArcadeBean`** (`arcade.xhtml`): pantalla principal con las
  estadísticas (XP, vidas, bono), el catálogo de minijuegos y la tienda
  para canjear XP.
- **`ArcadeJuegoBean`** (`@ViewScoped`, `arcadeJuego.xhtml?juego=...`):
  ejecuta la partida en sí — una ronda a la vez, con retroalimentación
  inmediata y pantalla de resultado final.

**Recompensas** (usan los campos que ya traía `Cuenta`:
`puntosExperiencia`, `vidasJuegoPrincipal`, `bonoCiudadAhorro`):
- Todos los juegos: **+10 XP por respuesta correcta**
- Ganar en **Ciudad del Ahorro** (≥70% de aciertos): además **+$15 de
  bono de ahorro**
- **Laberinto de Deudas**: ganar da **+1 vida**; perder (menos del 70% de
  aciertos) **resta 1 vida**
- **Tienda**: canjea XP por una vida extra (50 XP), un bono de $20 (80 XP)
  o una recarga completa de vidas (120 XP)

## Posibles mejoras futuras

Con la Zona Arcade ya implementada, la funcionalidad tiene paridad
completa con el proyecto original de consola. Ideas para seguir:
usar `f:ajax` en la partida del arcade para que cada ronda no recargue la
página completa, y añadir más preguntas a cada banco.

## Estructura

```
docker-compose.yml          -> contenedor de PostgreSQL
src/main/liberty/config/    -> server.xml (configuración de Open Liberty)
src/main/resources/META-INF/persistence.xml -> conexión JPA/Hibernate
src/main/java/edu/unl/cc/
  dominio/     -> entidades JPA (idénticas a la versión Spring)
  repositorio/ -> UsuarioRepositorio (repositorio + productor de EntityManager)
  web/         -> managed beans (@Named) consolidados, filtro de seguridad,
                  listener de arranque, servicio de correo
src/main/webapp/
  WEB-INF/template/  -> layout.xhtml, sidebar.xhtml, avatar.xhtml (Facelets)
  resources/css/     -> estilos (mismo tema "FinFlow")
  admin/panel.xhtml  -> dashboard + usuarios + detalle (?vista=)
  cuenta/configForm.xhtml -> saldo + límite (?campo=)
  movimientoForm.xhtml    -> ingreso + gasto (?tipo=)
  acceso.xhtml             -> registro + login (?modo=)
  estado.xhtml             -> bloqueado + error (?tipo=)
  arcade.xhtml             -> Zona Arcade: catálogo + tienda
  arcadeJuego.xhtml        -> partida de cualquier minijuego (?juego=)
  *.xhtml                  -> resto de páginas (index, menu, saldo, presupuesto, recuperar)
```
