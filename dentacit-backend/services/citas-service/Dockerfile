FROM php:8.3-apache

# Extensión para MySQL; todas las rutas van a public/index.php
RUN docker-php-ext-install pdo_mysql \
    && echo 'FallbackResource /index.php' > /etc/apache2/conf-enabled/dentacit.conf \
    && echo 'SetEnvIf Authorization "(.*)" HTTP_AUTHORIZATION=$1' >> /etc/apache2/conf-enabled/dentacit.conf \
    && sed -ri 's!/var/www/html!/var/www/html/public!g' /etc/apache2/sites-available/000-default.conf \
    && echo 'PassEnv DB_HOST DB_PORT DB_NAME DB_USER DB_PASS JWT_SECRET INTERNAL_KEY CATALOGO_URL NOTIF_URL DURACION_CITA_MIN TZ' >> /etc/apache2/conf-enabled/dentacit.conf

COPY src/ /var/www/html/src/
COPY public/ /var/www/html/public/

EXPOSE 80
