package es.ugr.giw;

import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.highlight.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BuscadorGUI extends JFrame {
    private JTextField txtBusqueda;
    private JTextArea areaResultados;

    // Variables globales de Lucene
    private DirectoryReader reader;
    private IndexSearcher searcher;
    private SpanishAnalyzer analyzer;

    // REQUISITO DEL GUION: Recibe la ruta del índice al instanciarse
    public BuscadorGUI(String indexPath) {
        super("Motor de Búsqueda Lucene - Práctica 3");
        inicializarLucene(indexPath);
        configurarVentana();
    }

    private void inicializarLucene(String indexPath) {
        try {
            FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
            reader = DirectoryReader.open(dir);
            searcher = new IndexSearcher(reader);
            analyzer = new SpanishAnalyzer(); // Usamos el analizador estándar para español
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al abrir el índice en: " + indexPath);
            System.exit(1);
        }
    }

    private void configurarVentana() {
        setLayout(new BorderLayout());
        JPanel panelNorte = new JPanel(new FlowLayout());
        txtBusqueda = new JTextField(40);
        JButton btnBuscar = new JButton("Buscar");

        panelNorte.add(new JLabel("Consulta:"));
        panelNorte.add(txtBusqueda);
        panelNorte.add(btnBuscar);

        areaResultados = new JTextArea();
        areaResultados.setEditable(false);
        areaResultados.setLineWrap(true);
        areaResultados.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(areaResultados);

        add(panelNorte, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Eventos de búsqueda
        btnBuscar.addActionListener(e -> ejecutarBusqueda(txtBusqueda.getText()));
        txtBusqueda.addActionListener(e -> ejecutarBusqueda(txtBusqueda.getText()));

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void ejecutarBusqueda(String textoConsulta) {
        if (textoConsulta.trim().isEmpty())
            return;
        areaResultados.setText("Buscando...\n\n");

        try {
            // INNOVACIÓN: Boosting (Dar más peso a la categoría)
            Map<String, Float> boosts = new HashMap<>();
            boosts.put("category", 2.0f);
            boosts.put("content", 1.0f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    new String[] { "category", "content" }, analyzer, boosts);
            Query query = parser.parse(textoConsulta);

            TopDocs foundDocs = searcher.search(query, 10);
            areaResultados.append("Resultados encontrados: " + foundDocs.totalHits.value + "\n\n");

            // INNOVACIÓN: Highlighting (Extraer el contexto de la palabra)
            SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("【", "】");
            Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));
            highlighter.setTextFragmenter(new SimpleFragmenter(150));

            for (ScoreDoc sd : foundDocs.scoreDocs) {
                Document d = searcher.storedFields().document(sd.doc);
                String contenidoCompleto = d.get("content");

                String snippet = highlighter.getBestFragment(analyzer, "content", contenidoCompleto);
                if (snippet == null) {
                    snippet = contenidoCompleto.substring(0, Math.min(150, contenidoCompleto.length())) + "...";
                } else {
                    snippet = "..." + snippet + "...";
                }

                areaResultados.append("SCORE: " + sd.score + "\n");
                areaResultados.append("CATEGORÍA: " + d.get("category") + " | URL: " + d.get("url") + "\n");
                areaResultados.append("CONTEXTO: " + snippet + "\n");
                areaResultados.append("--------------------------------------------------\n");
            }
        } catch (Exception e) {
            areaResultados.setText("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // REQUISITO DEL GUION: Leer ruta desde argumentos
        if (args.length < 1) {
            System.out.println("Uso: java BuscadorGUI <carpeta_indice>");
            return;
        }

        String indexPath = args[0];
        SwingUtilities.invokeLater(() -> new BuscadorGUI(indexPath).setVisible(true));
    }
}