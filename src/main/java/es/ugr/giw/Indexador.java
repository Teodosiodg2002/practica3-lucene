package es.ugr.giw;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Indexador {
    public static void main(String[] args) {
        // REQUISITO DEL GUION: 3 Argumentos
        if (args.length < 3) {
            System.out.println("Uso: java Indexador <archivo_csv> <fichero_stopwords> <carpeta_indice>");
            return;
        }

        String csvFile = args[0];
        String stopwordsFile = args[1];
        String indexPath = args[2];

        try {
            // 1. Cargar las palabras vacías (stopwords) desde el archivo de texto
            // proporcionado
            List<String> lineasStopwords = Files.readAllLines(Paths.get(stopwordsFile));
            CharArraySet stopSet = new CharArraySet(lineasStopwords, true);

            // 2. Crear el analizador inyectándole nuestras stopwords
            SpanishAnalyzer analyzer = new SpanishAnalyzer(stopSet);

            // 3. Abrir directorio y configurar IndexWriter
            FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            // Usamos try-with-resources para asegurar el cierre de flujos
            try (IndexWriter writer = new IndexWriter(dir, config);
                    Reader in = new FileReader(csvFile);
                    CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in)) {

                int count = 0;
                // Iterar el CSV e indexar
                for (CSVRecord record : parser) {
                    Document doc = new Document();
                    doc.add(new StringField("url", record.get("url"), Field.Store.YES));
                    doc.add(new StringField("category", record.get("Type"), Field.Store.YES));
                    doc.add(new TextField("content", record.get("news"), Field.Store.YES));

                    writer.addDocument(doc);
                    count++;
                }
                System.out.println("¡Éxito! Se han indexado " + count + " noticias.");
            }

        } catch (Exception e) {
            System.err.println("Error durante la indexación: " + e.getMessage());
        }
    }
}