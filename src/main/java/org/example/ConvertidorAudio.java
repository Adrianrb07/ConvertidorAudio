package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.*;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Esta clase es responsable de convertir archivos de audio de un formato a otro.
 */
public class ConvertidorAudio {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertidorAudio.class);

    /**
     * Convierte archivos de audio de un formato a otro.
     *
     * @param origen         El directorio que contiene los archivos fuente.
     * @param destino        El directorio donde se guardarán los archivos convertidos.
     * @param formatoOrigen  El formato de los archivos fuente.
     * @param formatoDestino El formato al que se convertirán los archivos.
     * @param codec          El codec que se utilizará para la conversión.
     */
    public static void convertir(File origen, File destino, String formatoOrigen, String formatoDestino, String codec) {
        // Comprueba si el origen y el destino son directorios
        if (!origen.isDirectory() || !destino.isDirectory()) {
            LOGGER.error("Uno de los parámetros no es un directorio o no existe.");
            return;
        }

        // Obtiene la lista de archivos a convertir
        File[] archivos = null;
        if (formatoOrigen.equals(".*")) {
            archivos = origen.listFiles((dir, name) -> name.matches(".*[.][^.]+$"));
        } else {
            archivos = origen.listFiles((dir, name) -> name.endsWith(formatoOrigen));
        }
        LOGGER.info("Archivos {} encontrados: {}", formatoOrigen, (archivos != null ? archivos.length : "Ninguno"));

        // Si no se encontraron archivos, retorna
        if (archivos == null || archivos.length == 0) {
            LOGGER.warn("No se encontraron archivos para convertir.");
            return;
        }

        // Crea un pool de hilos para las tareas de conversión
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Para cada archivo, envía una tarea de conversión al pool de hilos
        Arrays.stream(archivos).forEach(archivo -> executor.submit(() -> {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec(codec);
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat(formatoDestino);
            attrs.setAudioAttributes(audio);

            File destinoArchivo = new File(destino, archivo.getName().replaceFirst("[.][^.]+$", "." + formatoDestino));
            LOGGER.info("Iniciando conversión de: {} a {}", archivo.getPath(), destinoArchivo.getPath());

            // Intenta convertir el archivo
            try {
                new Encoder().encode(new MultimediaObject(archivo), destinoArchivo, attrs);
                LOGGER.info("Convertido: {} a {}", archivo.getName(), destinoArchivo.getName());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error al convertir el archivo: {}. Argumento ilegal.", archivo.getName(), e);
            } catch (InputFormatException e) {
                LOGGER.error("Error al convertir el archivo: {}. Formato de entrada no soportado.", archivo.getName(), e);
            } catch (EncoderException e) {
                LOGGER.error("Error al convertir el archivo: {}. Problema con el codificador.", archivo.getName(), e);
            }
        }));

        // Cierra el executor y espera a que todas las tareas terminen
        executor.shutdown();
        //noinspection StatementWithEmptyBody
        while (!executor.isTerminated()) {
            // Espera a que todos los trabajos de conversión finalicen
        }
        LOGGER.info("Todas las conversiones han terminado.");
    }

    /**
     * El método principal para probar la conversión de audio.
     *
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        File origen = new File("Ruta de la carpeta de origen");
        File destino = new File("Ruta de la carpeta de destino");
        if (!destino.exists()) {
            boolean isCreated = destino.mkdir();
            if (!isCreated) {
                LOGGER.error("Error al crear el directorio de destino.");
                return;
            }
        }
        convertir(origen, destino, ".*", "mp3", "libmp3lame");
        // convertir(origen, destino, ".mp3", "wav", "pcm_s16le");
    }
}