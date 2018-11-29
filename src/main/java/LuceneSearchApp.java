/*
 * Skeleton class for the Lucene search program implementation
 */
//package ir_course;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
public class LuceneSearchApp {

//    static String indexPath = "/home/klos/studia/2sem/lucene2/index_bbc";
    static String indexPath = "index_bbc";

    public LuceneSearchApp() {

    }

    public void index(List<RssFeedDocument> docs) {

        Path iPath = Paths.get(indexPath);
        Directory dir;

        try {
            dir = FSDirectory.open(iPath);
        } catch (Exception e) {
            return;
        }

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iWC = new IndexWriterConfig(analyzer);
        iWC.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iW;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            iW = new IndexWriter(dir, iWC);
        } catch (Exception e) {
            return;
        }

        for (RssFeedDocument rss: docs) {
            Document d = new Document();

            Field title = new TextField("title", rss.getTitle(), Field.Store.YES);
            d.add(title);

            Field desc = new TextField("description", new StringReader(rss.getDescription()));
            d.add(desc);

            Field date = new StringField("date", simpleDateFormat.format(rss.getPubDate()), Field.Store.YES);
            d.add(date);

            try {
                iW.addDocument(d);
            } catch (Exception e) {

            }
        }
        try {
            iW.commit();
            iW.close();
        } catch (Exception e) {

        }
    }

    public String getDateQuery(String start, String stop) {
        String result = "";
        if (start != null || stop != null) {
            String start_ = start != null ? start : "*";
            String stop_ = stop != null ? stop : "*";
            result = String.format("date:[%s TO %s]", start_, stop_);
        }
        return result;
    }

    public Query queryOfLists(List<String> inTitle, List<String> notInTitle, List<String> inDescription,
                              List<String> notInDescription, String startDate, String endDate) throws ParseException {

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Analyzer analyzer = new StandardAnalyzer();

        QueryParser titleParser = new QueryParser("title", analyzer);
        QueryParser descriptionParser = new QueryParser("description", analyzer);
        QueryParser dateParser = new QueryParser("date", analyzer);

        if (inTitle != null) {
//            for (String title : inTitle) {
                builder.add(titleParser.parse(String.join(" AND ", inTitle)), BooleanClause.Occur.MUST);
//            }
        }
        if (notInTitle != null) {
//            for (String title : notInTitle) {
                builder.add(titleParser.parse(String.join(" OR ", notInTitle)), BooleanClause.Occur.MUST_NOT);
//            }
        }
        if (inDescription != null) {
//            for (String description : notInDescription) {
                builder.add(descriptionParser.parse(String.join(" AND ", inDescription)), BooleanClause.Occur.MUST);
//            }
        }
        if (notInDescription != null) {
//            for (String title : notInTitle) {
                builder.add(descriptionParser.parse(String.join(" OR ", notInDescription)), BooleanClause.Occur.MUST_NOT);
//            }
        }
        String dateQuery = getDateQuery(startDate, endDate);
        if  (dateQuery != "") {
            builder.add(dateParser.parse(dateQuery), BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription,
                               List<String> notInDescription, String startDate, String endDate)
            throws ParseException, IOException {

        printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

        List<String> results = new LinkedList<String>();
        Query query;
        try {
            query = queryOfLists(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);
        } catch (Exception e) {
            return null;
        }
        // implement the Lucene search here

        Path path = Paths.get(indexPath);
        Directory dir;
        try {
            dir = FSDirectory.open(path);
        } catch (Exception e) {
            return null;
        }
        IndexReader indexReader;
        try {
            indexReader = DirectoryReader.open(dir);
        } catch (Exception e) {
            return null;
        }
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs topDocs;
        try {
            topDocs = indexSearcher.search(query, 20);
        } catch (Exception e) {
            return null;
        }

        ScoreDoc hits[] = topDocs.scoreDocs;

        for (ScoreDoc hit: hits) {
            results.add(indexReader.document(hit.doc).getField("title").stringValue());
        }

        return results;
    }

    public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
        System.out.print("Search (");
        if (inTitle != null) {
            System.out.print("in title: "+inTitle);
            if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (notInTitle != null) {
            System.out.print("not in title: "+notInTitle);
            if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (inDescription != null) {
            System.out.print("in description: "+inDescription);
            if (notInDescription != null || startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (notInDescription != null) {
            System.out.print("not in description: "+notInDescription);
            if (startDate != null || endDate != null)
                System.out.print("; ");
        }
        if (startDate != null) {
            System.out.print("startDate: "+startDate);
            if (endDate != null)
                System.out.print("; ");
        }
        if (endDate != null)
            System.out.print("endDate: "+endDate);
        System.out.println("):");
    }

    public void printResults(List<String> results) {
        if (results.size() > 0) {
            Collections.sort(results);
            for (int i=0; i<results.size(); i++)
                System.out.println(" " + (i+1) + ". " + results.get(i));
        }
        else
            System.out.println(" no results");
    }

    public static void main(String[] args) throws IOException, ParseException {

        args = new String[1];
//        args[0] = "/home/klos/studia/2sem/lucene2/bbc/bbc_rss_feed.xml";
        args[0] = "bbc/bbc_rss_feed.xml";

        if (args.length > 0) {
            LuceneSearchApp engine = new LuceneSearchApp();

            RssFeedParser parser = new RssFeedParser();
            parser.parse(args[0]);
            List<RssFeedDocument> docs = parser.getDocuments();

            engine.index(docs);

            List<String> inTitle;
            List<String> notInTitle;
            List<String> inDescription;
            List<String> notInDescription;
            List<String> results;

            // 1) search documents with words "kim" and "korea" in the title
            inTitle = new LinkedList<String>();
            inTitle.add("kim");
            inTitle.add("korea");
            results = engine.search(inTitle, null, null, null, null, null);
            engine.printResults(results);

            // 2) search documents with word "kim" in the title and no word "korea" in the description
            inTitle = new LinkedList<String>();
            notInDescription = new LinkedList<String>();
            inTitle.add("kim");
            notInDescription.add("korea");
            results = engine.search(inTitle, null, null, notInDescription, null, null);
            engine.printResults(results);

            // 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
            inTitle = new LinkedList<String>();
            inTitle.add("us");
            notInTitle = new LinkedList<String>();
            notInTitle.add("dawn");
            inDescription = new LinkedList<String>();
            inDescription.add("american");
            inDescription.add("confession");
            results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
            engine.printResults(results);

            // 4) search documents whose publication date is 2011-12-18
            results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
            engine.printResults(results);

            // 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
            inTitle = new LinkedList<String>();
            inTitle.add("video");
            results = engine.search(inTitle, null, null, null, "2000-01-01", null);
            engine.printResults(results);

            // 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
            notInDescription = new LinkedList<String>();
            notInDescription.add("canada");
            notInDescription.add("iraq");
            notInDescription.add("israel");
            results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
            engine.printResults(results);
        }
        else
            System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
    }
}
