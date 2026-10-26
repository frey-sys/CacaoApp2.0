document.querySelectorAll('[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
});

const locateButton = document.querySelector('[data-geolocate]');
if (locateButton) {
    locateButton.addEventListener('click', () => {
        const message = document.querySelector('[data-location-message]');
        if (!navigator.geolocation) {
            message.textContent = 'Este navegador no permite obtener la ubicación. Ingresa las coordenadas manualmente.';
            return;
        }
        locateButton.disabled = true;
        message.textContent = 'Obteniendo ubicación…';
        navigator.geolocation.getCurrentPosition(
            ({ coords }) => {
                document.querySelector('#latitud').value = coords.latitude.toFixed(6);
                document.querySelector('#longitud').value = coords.longitude.toFixed(6);
                message.textContent = 'Ubicación obtenida correctamente.';
                locateButton.disabled = false;
            },
            () => {
                message.textContent = 'No se pudo obtener la ubicación. Revisa los permisos o ingrésala manualmente.';
                locateButton.disabled = false;
            },
            { enableHighAccuracy: true, timeout: 10000 },
        );
    });
}

const routeStartButton = document.querySelector('[data-route-start]');
if (routeStartButton) {
    let count = 0;
    let recordingTimer;
    const countDisplay = document.querySelector('[data-route-count]');
    const pointsInput = document.querySelector('[data-route-points]');
    const status = document.querySelector('[data-route-status]');
    routeStartButton.addEventListener('click', () => {
        if (recordingTimer) {
            window.clearInterval(recordingTimer);
            recordingTimer = undefined;
            routeStartButton.textContent = '▶ Continuar grabación';
            status.textContent = 'Grabación pausada. Puedes guardar los puntos registrados.';
            return;
        }
        routeStartButton.textContent = 'Ⅱ Pausar grabación';
        status.textContent = 'Grabando recorrido…';
        recordingTimer = window.setInterval(() => {
            count += 1;
            countDisplay.textContent = count;
            pointsInput.value = count;
        }, 3000);
    });
    document.querySelector('[data-route-form]').addEventListener('submit', () => {
        if (recordingTimer) window.clearInterval(recordingTimer);
    });
}

const qualityDate = document.querySelector('#fecha');
if (qualityDate && !qualityDate.value) {
    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    qualityDate.value = today.toISOString().slice(0, 10);
}
