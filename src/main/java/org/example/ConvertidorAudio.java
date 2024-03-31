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

public class ConvertidorAudio {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertidorAudio.class);

    // Formatos soportados: mp3, wav, ogg, flac, aac, m4a, wma, ac3, mp2, amr, pcm_s16le, pcm_s24le, pcm_u8, pcm_s8,
    // pcm_mulaw, pcm_alaw, adpcm_ima_wav, adpcm_ms, adpcm_swf, adpcm_yamaha, vorbis, libmp3lame, libvorbis, libfaac,
    // libfaad, libx264, libxvid, mpeg2video, mpeg4, flv, mov, avi, asf, mkv, mp4, 3gp, webm, mjpeg, mpegts, mpegps,
    // flv, gif, swf, mp3, wav, ogg, flac, aac, m4a, wma, ac3, mp2, amr, pcm_s16le, pcm_s24le, pcm_u8, pcm_s8, pcm_mulaw,
    // pcm_alaw, adpcm_ima_wav, adpcm_ms, adpcm_swf, adpcm_yamaha, vorbis, libmp3lame, libvorbis, libfaac, libfaad,
    // libx264, libxvid, mpeg2video, mpeg4, flv, mov, avi, asf, mkv, mp4, 3gp, webm, mjpeg, mpegts, mpegps, flv, gif, swf
    public static void convertir(File origen, File destino, String formatoOrigen, String formatoDestino, String codec) {
        if (!origen.isDirectory() || !destino.isDirectory()) {
            LOGGER.error("Uno de los parámetros no es un directorio o no existe.");
            return;
        }

        File[] archivos = null;
        if (formatoOrigen.equals(".*")) {
            archivos = origen.listFiles((dir, name) -> name.matches(".*[.][^.]+$"));
        } else {
            archivos = origen.listFiles((dir, name) -> name.endsWith(formatoOrigen));
        }
        LOGGER.info("Archivos {} encontrados: {}", formatoOrigen, (archivos != null ? archivos.length : "Ninguno"));
        
        if (archivos == null || archivos.length == 0) {
            LOGGER.warn("No se encontraron archivos para convertir.");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

        executor.shutdown();
        while (!executor.isTerminated()) {
            // Espera a que todos los trabajos de conversión finalicen
        }
        LOGGER.info("Todas las conversiones han terminado.");
    }

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