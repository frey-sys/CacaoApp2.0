# CacaoAPP2.0

Aplicación web independiente en Java con Spring MVC, Thymeleaf y Hibernate. Incluye registro e inicio de sesión, gestión de fincas, rutas y calidad, además de una prueba CRUD por consola.

## Requisitos

- JDK 17 o superior.
- Maven 3.9 o superior.
- No necesitas instalar ni iniciar MySQL para probarla: por defecto usa H2 en memoria.

## Probar ahora sin MySQL

Desde PowerShell, ejecuta el CRUD de consola:

```powershell
cd C:\Users\FREYMAR\Desktop\CacaoAPP2.0
mvn clean compile exec:java
```

La consola ejecuta CREATE, READ, UPDATE y DELETE sobre las cinco entidades y muestra las etiquetas `[CREATE]`, `[READ]`, `[UPDATE]` y `[DELETE]`. H2 funciona en memoria; los datos se reinician al terminar el proceso.

Para abrir la interfaz web, inicia Spring Boot:

```powershell
mvn spring-boot:run
```

Abre `http://localhost:8080`, crea una cuenta e inicia sesión. Mientras Spring Boot esté activo, los datos se guardan en H2 en memoria. Al detenerlo, se borran. El navegador puede solicitar permiso de ubicación al registrar fincas; también puedes introducir las coordenadas manualmente. El contador de rutas es demostrativo, no registra GPS real.

Detén la aplicación web con `Ctrl+C` en la terminal.

## Conectar MySQL más adelante

Instala e inicia MySQL, crea la base de datos:

```sql
CREATE DATABASE cacaoapp_db CHARACTER SET utf8mb4;
```

En la misma terminal donde ejecutes Maven, configura las credenciales:

```powershell
$env:HIBERNATE_DB_USER = "root"
$env:HIBERNATE_DB_PASSWORD = "tu_contraseña"
$env:HIBERNATE_JDBC_URL = "jdbc:mysql://localhost:3306/cacaoapp_db?useSSL=false&serverTimezone=UTC"
```

El controlador y dialecto se seleccionan según la URL JDBC. Los datos de H2 no se copian automáticamente a MySQL.

`hbm2ddl.auto=update` se mantiene para desarrollo. Para producción, usa migraciones y credenciales gestionadas fuera del código.
