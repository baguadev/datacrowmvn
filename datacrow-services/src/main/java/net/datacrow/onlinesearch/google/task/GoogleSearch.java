package net.datacrow.onlinesearch.google.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import net.datacrow.core.http.HttpConnection;
import net.datacrow.core.http.HttpConnectionUtil;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.SearchTaskUtilities;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.utilities.StringUtils;
import org.apache.log4j.Logger;

public class GoogleSearch
        extends SearchTask
{
    private static Logger logger = Logger.getLogger(GoogleSearch.class.getName());

    public GoogleSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query)
    {
        super(listener, server, null, mode, query);
    }

    protected DcObject getItem(Object key, boolean full)
            throws Exception
    {
        return (DcObject)key;
    }

    protected DcObject getItem(URL url)
            throws Exception
    {
        return null;
    }

    public String getWhiteSpaceSubst()
    {
        return "+";
    }

    protected void preSearchCheck()
    {
        SearchTaskUtilities.checkForIsbn(this);
    }

    private void setDescription(String googleBook, Book book)
    {
        String description = getValue("subtitle", googleBook);

        String text = getValue("description", googleBook);
        text = (text == null) || (text.length() == 0) ? getValue("textSnippet", googleBook) : text;
        if ((text != null) && (text.length() > 0))
        {
            description = description + (description.length() > 0 ? "\n\n" : "");
            description = description + text;
        }
        book.setValue(151, description);
    }

    private void setYear(String googleBook, Book book)
    {
        String publishedDate = getValue("publishedDate", googleBook);
        if ((publishedDate != null) && (publishedDate.length() > 0)) {
            try
            {
                publishedDate = publishedDate.contains("-") ? publishedDate.substring(0, publishedDate.indexOf("-")) : publishedDate;
                book.setValue(152, Long.valueOf(publishedDate));
            }
            catch (Exception e)
            {
                logger.debug("Could not parse publishdate for " + book + ", value: " + publishedDate, e);
            }
        }
    }

    private void setRating(String googleBook, Book book)
    {
        String averageRating = getValue("averageRating", googleBook);
        if ((averageRating != null) && (averageRating.length() > 0)) {
            try
            {
                float rating = Float.valueOf(averageRating).floatValue() * 2.0F;
                book.setValue(154, Integer.valueOf(Math.round(Float.valueOf(rating).floatValue())));
            }
            catch (Exception e)
            {
                logger.debug("Could not parse rating for " + book, e);
            }
        }
    }

    private void setIsbn(String googleBook, Book book)
    {
        String industryIdentifiers = getValue("industryIdentifiers", googleBook);
        if ((industryIdentifiers != null) && (industryIdentifiers.contains("ISBN_13")))
        {
            String isbn13 = industryIdentifiers.substring(industryIdentifiers.indexOf("ISBN_13"));
            isbn13 = getValue("identifier", isbn13);
            book.setValue(9, isbn13);
        }
    }

    private void setAuthors(String googleBook, Book book)
    {
        String authors = getValue("authors", googleBook);
        if ((authors != null) && (authors.length() > 0)) {
            for (String author : StringUtils.getValuesBetween("\"", "\"", authors)) {
                if ((!author.startsWith(",")) && (author.length() != 0)) {
                    book.createReference(2, author);
                }
            }
        }
    }

    private void setCategories(String googleBook, Book book)
    {
        String categories = getValue("categories", googleBook);
        if ((categories != null) && (categories.length() > 0)) {
            for (String category : StringUtils.getValuesBetween("\"", "\"", categories)) {
                if ((!category.startsWith(",")) && (category.length() != 0)) {
                    book.createReference(4, category);
                }
            }
        }
    }

    private void setPages(String googleBook, Book book)
    {
        String pageCount = getValue("pageCount", googleBook);
        if ((pageCount != null) && (pageCount.length() > 0)) {
            try
            {
                book.setValue(15, Long.valueOf(pageCount));
            }
            catch (NumberFormatException nfe)
            {
                logger.debug("Cannot determine the number of pages for " + book + ", value " + pageCount, nfe);
            }
        }
    }

    private void setImages(String googleBook, Book book)
    {
        String link = getValue("thumbnail", googleBook);
        try
        {
            if ((link != null) && (link.length() > 0))
            {
                byte[] b = HttpConnectionUtil.retrieveBytes(link);
                if ((b != null) && (b.length > 50)) {
                    book.setValue(6, b);
                }
            }
        }
        catch (Exception e)
        {
            logger.debug("Cannot download image for " + book + ", value " + link, e);
        }
    }

    protected Collection<Object> getItemKeys()
            throws Exception
    {
        Collection<Object> keys = new ArrayList();

        URL url = new URL("https://www.googleapis.com/books/v1/volumes?q=" + getQuery());

        HttpConnection connection = HttpConnectionUtil.getConnection(url);
        String result = connection.getString("UTF-8");
        if (logger.isDebugEnabled()) {}
        Collection<String> googleBooks = StringUtils.getValuesBetween("\"books#volume\"", "\"books#volume\"", result);
        for (String googleBook : googleBooks)
        {
            Book book = new Book();

            String googleID = getValue("id", googleBook);

            book.addExternalReference("GOOGLE", googleID);
            book.setValue(208, getValue("selfLink", googleBook));
            book.setValue(150, getValue("title", googleBook));
            book.setValue(3, "http://books.google.com/books?id=" + googleID);

            book.createReference(1, getValue("publisher", googleBook));

            setDescription(googleBook, book);
            setYear(googleBook, book);
            setRating(googleBook, book);
            setIsbn(googleBook, book);
            setAuthors(googleBook, book);
            setCategories(googleBook, book);
            setPages(googleBook, book);
            setImages(googleBook, book);

            keys.add(book);
        }
        return keys;
    }

    private String getValue(String tag, String text)
    {
        int start = text.indexOf("\"" + tag + "\": ");

        String value = "";
        if (start > -1)
        {
            value = text.substring(start + tag.length() + 4);
            if (value.startsWith("[")) {
                value = value.substring(0, value.indexOf("]"));
            } else if (value.startsWith("\"")) {
                value = value.substring(1, value.indexOf("\n"));
            } else {
                value = value.substring(0, value.indexOf("\n"));
            }
        }
        if (value.endsWith(",")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        value = value.trim();

        return value;
    }
}
