

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;


public class LuceneLab6 {
    //TODO follows TODOs; there four places that should be filled with code
    //according to the instructions given in Lab6.pdf

    //directory where the index would be placed in
    //provided by the user as args[1]; set in main()
    public static String indexPath;

    //TODO create the index, fill it with documents (use indexDoc function), close the index
    // use: indexWriter to create an index of selected files
    public static void createIndex(String path) throws Exception {

        Path iPath = Paths.get(indexPath);
        Directory directory = FSDirectory.open(iPath);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        File plik = new File(path);

        String[] pliki = plik.list();

        for (String doc:pliki) {
            indexWriter.addDocument(indexDoc(path + "/" + doc));
        }
        indexWriter.close();

    }

    //call this function in createIndex() to create Documents that would be subsequently added to the index
    public static Document indexDoc(String docPath) {
        System.out.println("indeksuje: " + docPath);

        FileInputStream file;
        try {
            file = new FileInputStream(docPath);
        } catch (Exception e) {
            return null;
        }
        Document document = new Document();

        Field pathField = new StringField("path", docPath, Field.Store.YES);
        document.add(pathField);

        Field contentField = new TextField("content", new InputStreamReader(file));
        document.add(contentField);

        return document;
    }

    public static IndexSearcher getIndexSearcher() throws IOException {
        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        return indexSearcher;
    }

    //TODO create objects of class: Analyzer, IndexSearcher, QueryParser, Query and Hits
    //for Analyzer use standard analyzer
    //for QueryParser indicate the fields to be analyzed
    //for Query you should parse "queryString" which is given as a parameter of the function
    //for TopDocs you should search results (indexSearcher) for a given query and return 5 best documents
    public static ScoreDoc[] processQuery(IndexSearcher indexSearcher, String queryString) throws IOException {

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser("content", analyzer);
        Query query;
        try {
            query = queryParser.parse(queryString);
        } catch (Exception e) {
            return null;
        }
        TopDocs topDocs = indexSearcher.search(query, 5);
        ScoreDoc hits[] = topDocs.scoreDocs;
        return hits;
    }

    public static void main(String [] args) {

        args = new String[2];
        args[0] = "/home/klos/studia/2sem/lucene2/Shakespeare";
        args[1] = "/home/klos/studia/2sem/lucene2/index";

        try {
            createIndex(args[0]);
        } catch (Exception e) {

        }

        if (args.length < 2) {
            System.out.println("java -cp lucene-core-2.2.0.jar:. BasicIRsystem texts_path index_path");
            System.out.println("need two args with paths to the collection of texts and to the directory where the index would be stored, respectively");
            System.exit(1);
        }
        try {
            String textsPath = args[0];
            indexPath = args[1];
            createIndex(textsPath);
            String query = " ";

            IndexSearcher indexSearcher = getIndexSearcher();

            //process queries until one writes "lab6"
            while (true) {
                Scanner sc = new Scanner(System.in);
                System.out.println("Please enter your query: (lab9 to quit)");
                query = sc.next();

                if (query.equals("lab9")) {break;} //to quit

                ScoreDoc hits[] = processQuery(indexSearcher, query);

                if (hits != null)
                {
                    System.out.println(hits.length + " result(s) found");

                    for (ScoreDoc hit: hits)
                    {
                        try {
                            System.out.println(indexSearcher.doc(hit.doc).getValues("path")[0] +
                                    " - " +
                                    String.format("%f", hit.score));


                        }
                        catch (Exception e) {
                            System.err.println("Unexpected exception");
                            System.err.println(e.toString());
                        }
                    }

                }
                else
                {
                    System.out.println("Processing the query still not implemented, heh?");
                }
            }

        } catch (Exception e) {
            System.err.println("Even more unexpected exception");
            System.err.println(e.toString());
        }
    }
}
