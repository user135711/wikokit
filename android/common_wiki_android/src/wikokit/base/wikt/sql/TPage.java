/* TPage.java - SQL operations with the table 'page' in SQLite Android 
 *              Wiktionary parsed database.
 *
 * Copyright (c) 2009-2012 Andrew Krizhanovsky <andrew.krizhanovsky at gmail.com>
 * Distributed under EPL/LGPL/GPL/AL/BSD multi-license.
 */

package wikokit.base.wikt.sql;

import wikokit.base.wikipedia.language.LanguageType;
//import wikokit.base.wikipedia.sql.Connect;

//import wikt.api.WTMeaning;
//import wikt.word.*;
//import wikipedia.util.StringUtil;

import java.util.List;
import java.util.ArrayList;

import java.sql.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** An operations with the table 'page' in MySQL wiktionary_parsed database. */
public class TPage {
    
    /** Unique page identifier. */
    private int id;

    /** Title of the wiki page, word. */
    private String page_title;

    /** Size of the page in words. */
    private int word_count;
    
    /** Size of the page as a number of wikified words at the page
     * (number of out-links). */
    private int wiki_link_count;

    /** true, if the page_title exists in Wiktionary
     * false, if the page_title exists only as a [[|wikified word]] */
    private boolean is_in_wiktionary;

    /** Hard redirect defined by #REDIRECT
     * @see TLangPOS.redirect_type and .lemma - a soft redirect. */
    private boolean is_redirect;

    /** Redirected page, i.e. target or destination page.
     * It is null for usual entries.
     *
     * Hard redirect defined by #REDIRECT",
     * @see TLangPOS.redirect_type and .lemma - a soft redirect.
     */
    private String  redirect_target;

    /** Array of language-POS with this page_title */
    private TLangPOS[] lang_pos;

    private final static TPage[]    NULL_TPAGE_ARRAY    = new TPage[0];
    private final static TLangPOS[] NULL_TLANGPOS_ARRAY = new TLangPOS[0];
    private final static String[]   NULL_STRING_ARRAY   = new String[0];
    
    public TPage(int _id,String _page_title,int _word_count,int _wiki_link_count,
                 boolean _is_in_wiktionary,
                 String _redirect_target)
    {
        id              = _id;
        page_title      = _page_title;
        word_count      = _word_count;
        wiki_link_count = _wiki_link_count;
        is_in_wiktionary = _is_in_wiktionary;

        is_redirect     = null != _redirect_target;
        redirect_target = _redirect_target;

        lang_pos        = NULL_TLANGPOS_ARRAY;
    }

    /*public void init() {
        id              = 0;
        page_title      = "";
        word_count      = 0;
        wiki_link_count = 0;
        is_in_wiktionary = false;
    }*/

    @Override
    public String toString() {
        return "id=" + id + "; page_title=" + page_title;
    }
    
    /** Gets unique ID from database */
    public int getID() {
        return id;
    }

    /** Gets title of the wiki page, word. */
    public String getPageTitle() {
        return page_title;
    }
    
    /** Gets number of words, size of the page in words. */
    public int getWordCount() {
        return word_count;
    }

    /** Gets number of out-links, size of the page as a number of wikified words. */
    public int getWikiLinkCount() {
        return wiki_link_count;
    }

    /** Returns true, if the page_title exists in Wiktionary. */
    public boolean isInWiktionary() {
        return is_in_wiktionary;
    }

    /** Returns true, if the page_title is a #REDIRECT in Wiktionary.
     * @see TLangPOS.redirect_type and .lemma - a soft redirect.
     */
    public boolean isRedirect() {
        return is_redirect;
    }
    
    /** Gets a redirected page, i.e. target or destination page.
     * It is null for usual entries.
     */
    public String getRedirect() {
        return redirect_target;
    }
    
    /** Sets array: language and part of speech.
     */
    public void setLangPOS(TLangPOS[] _lang_pos) {
        lang_pos = _lang_pos;
    }
    
    /** Gets array of language-POS with this page_title (language and part of speech).
     */
    public TLangPOS[] getLangPOS() {
        return lang_pos;
    }

    /** Gets ID of a record or inserts record (if it is absent)
     * into the table 'page'.<br><br>
     * 
     * @param page_title   title of wiki page
     * @param word_count   size of the page in words
     * @param wiki_link_count number of wikified words at the page
     * @param is_in_wiktionary true, if the page_title exists in Wiktionary
     * @param redirect_target redirected (target, destination) page,
     *                         it is null for usual entries
     */
    /*public static TPage getOrInsert (Connect connect,String _page_title,
                            int _word_count,int _wiki_link_count,
                            boolean _is_in_wiktionary,String _redirect_target) {
        
        TPage p = TPage.get(connect, _page_title);
        if(null == p)
            p = TPage.insert(connect, _page_title, _word_count, _wiki_link_count,
                            _is_in_wiktionary, _redirect_target);
        else {
            if( p.is_in_wiktionary != _is_in_wiktionary) {
                TPage.setIsInWiktionary(connect, _page_title, _is_in_wiktionary);
                p.is_in_wiktionary = _is_in_wiktionary;
            }
        }
        return p;
    }*/

    /** Inserts record into the table 'page'.<br><br>
     * INSERT INTO page (page_title,word_count,wiki_link_count,is_in_wiktionary) VALUES ("apple",1,2,TRUE);
     * 
     * or with redirect:
     * INSERT INTO page (page_title,word_count,wiki_link_count,is_in_wiktionary,is_redirect,redirect_target) VALUES ("apple",1,2,TRUE,TRUE,"test_neletnwi");
     * @param page_title   title of wiki page
     * @param word_count   size of the page in words
     * @param wiki_link_count number of wikified words at the page
     * @param is_in_wiktionary true, if the page_title exists in Wiktionary
     * @param redirect_target redirected (target, destination) page,
     *                         it is null for usual entries
     */
    /*public static TPage insert (Connect connect,String page_title,int word_count,int wiki_link_count,
            boolean is_in_wiktionary,String redirect_target) {
        
        StringBuilder str_sql = new StringBuilder();
        TPage page = null;
        boolean is_redirect = null != redirect_target && redirect_target.length() > 0;
        try
        {
            Statement s = connect.conn.createStatement ();
            try {
                str_sql.append("INSERT INTO page (page_title,word_count,wiki_link_count,is_in_wiktionary");

                if(is_redirect)
                    str_sql.append(",is_redirect,redirect_target");

                str_sql.append(") VALUES (\"");
                String safe_title = PageTableBase.convertToSafeStringEncodeToDBWunderscore(connect, page_title);
                str_sql.append(safe_title);
                str_sql.append("\",");
                str_sql.append(word_count);
                str_sql.append(",");
                str_sql.append(wiki_link_count);
                str_sql.append(",");
                str_sql.append(is_in_wiktionary);

                if(is_redirect) {// ,TRUE,"test_neletnwi"
                    str_sql.append(",TRUE,\"");
                    str_sql.append(PageTableBase.convertToSafeStringEncodeToDBWunderscore(connect,
                                   redirect_target));
                    str_sql.append("\"");
                }

                str_sql.append(")");
                if(s.executeUpdate (str_sql.toString()) > 0) {
                    ResultSet rs = connect.conn.prepareStatement( "SELECT LAST_INSERT_ID() AS id" ).executeQuery();
                    try {
                        if (rs.next ()) {
                            page = new TPage(rs.getInt("id"), page_title, word_count, wiki_link_count,
                                             is_in_wiktionary, redirect_target);
                            //System.out.println("TPage insert()):: id=" + rs.getInt("id") +
                            //    "; page_title='" + page_title + "'");
                        }
                    } finally {
                        rs.close();
                    }
                }
            } finally {
                s.close();
            }
        }catch(SQLException ex) {
            System.err.println("SQLException (TPage.insert()):: sql='" + str_sql.toString() + "' " + ex.getMessage());
        }
        return page;
    }*/

    /** Update the field 'is_in_wiktionary' in the table 'page',
     * record is identiied by 'page_title'.<br><br>
     * UPDATE page SET is_in_wiktionary=1 WHERE page_title="centi-";
     *
     * @param page_title   unique title of an wiki page
     * @param is_in_wiktionary true, if the page_title exists in Wiktionary
     */
    /*public static void setIsInWiktionary (Connect connect,String page_title,
                                            boolean is_in_wiktionary)
    {
        StringBuilder str_sql = new StringBuilder();
        try
        {
            Statement s = connect.conn.createStatement ();
            try {
                if(is_in_wiktionary)
                    str_sql.append("UPDATE page SET is_in_wiktionary=1");
                else
                    str_sql.append("UPDATE page SET is_in_wiktionary=0");

                str_sql.append(" WHERE page_title=\"");
                String safe_title = PageTableBase.convertToSafeStringEncodeToDBWunderscore(connect, page_title);
                str_sql.append(safe_title);
                str_sql.append("\"");
                s.executeUpdate (str_sql.toString());
            } finally {
                s.close();
            }
        }catch(SQLException ex) {
            System.err.println("SQLException (wikt_parsed TPage.setIsInWiktionary()):: page_title='"+page_title+"'; sql='" + str_sql.toString() + "' " + ex.getMessage());
        }
    }*/

    /** Selects row from the table 'page' by the page_title.
     * (1) Without conversion into the database encoding for SQLite.
     * (2) With conversion for MySQL (skip here, Thanks God, we are using SQLite in Android)
     *
     *  SELECT id,word_count,wiki_link_count,is_in_wiktionary,is_redirect,redirect_target FROM page WHERE page_title="apple";
     *
     * @param  page_title  title of Wiktionary article
     * @return null if page_title is absent
     */
    public static TPage get (SQLiteDatabase db,String _page_title)
    {
        if(null == _page_title || _page_title.length() == 0)
            return null;
                              
        TPage tp = null;
        Cursor c = db.query("page", 
                new String[] { "id", "word_count", "wiki_link_count", 
                "is_in_wiktionary", "is_redirect", "redirect_target"}, 
                "page_title=\"" + _page_title + "\"", 
                null, null, null, null);
                
        if (c.moveToFirst()) {
            int i_id = c.getColumnIndexOrThrow("id");
            int i_word_count = c.getColumnIndexOrThrow("word_count");
            int i_wiki_link_count = c.getColumnIndexOrThrow("wiki_link_count");
            int i_is_in_wiktionary = c.getColumnIndexOrThrow("is_in_wiktionary");
            int i_is_redirect = c.getColumnIndexOrThrow("is_redirect");
            int i_redirect_target = c.getColumnIndexOrThrow("redirect_target");

            int _id = c.getInt(i_id);
            int _word_count = c.getInt(i_word_count);
            int _wiki_link_count = c.getInt(i_wiki_link_count);
            boolean _is_in_wiktionary = 0 != c.getInt(i_is_in_wiktionary);
            boolean _is_redirect  = 0 != c.getInt(i_is_redirect);
            String _redirect_target = _is_redirect ? c.getString(i_redirect_target) : null;

            tp = new TPage(_id, _page_title, _word_count, _wiki_link_count,
                    _is_in_wiktionary, _redirect_target);
        }
        if (c != null && !c.isClosed()) {
           c.close();
        }
        return tp;
    }
    
     /** Selects row from the table 'page' by the page ID.
      *
      * SELECT page_title,word_count,wiki_link_count,is_in_wiktionary,is_redirect,redirect_target FROM page WHERE id=1;
      *
      * @param  id  ID of Wiktionary article's title in the table 'page'
      * @return null if page_title is absent
      */
    public static TPage getByID (SQLiteDatabase db,int _id) {

        if(_id <= 0)
            return null;
        
        TPage tp = null;
        Cursor c = db.query("page", 
                new String[] { "page_title", "word_count", "wiki_link_count", 
                "is_in_wiktionary", "is_redirect", "redirect_target"}, 
                "id=" + _id, 
                null, null, null, null);
                
        if (c.moveToFirst()) {
            int i_page_title = c.getColumnIndexOrThrow("page_title");
            int i_word_count = c.getColumnIndexOrThrow("word_count");
            int i_wiki_link_count = c.getColumnIndexOrThrow("wiki_link_count");
            int i_is_in_wiktionary = c.getColumnIndexOrThrow("is_in_wiktionary");
            int i_is_redirect = c.getColumnIndexOrThrow("is_redirect");
            int i_redirect_target = c.getColumnIndexOrThrow("redirect_target");

            String _page_title = c.getString(i_page_title);
            int _word_count = c.getInt(i_word_count);
            int _wiki_link_count = c.getInt(i_wiki_link_count);
            boolean _is_in_wiktionary = 0 != c.getInt(i_is_in_wiktionary);
            boolean _is_redirect      = 0 != c.getInt(i_is_redirect);
            String _redirect_target = _is_redirect ? c.getString(i_redirect_target) : null;

            tp = new TPage(_id, _page_title, _word_count, _wiki_link_count,
                    _is_in_wiktionary, _redirect_target);
        }
        if (c != null && !c.isClosed()) {
           c.close();
        }
        return tp;
    }

    /** Selects row from the table 'page', WHERE page_title starts from 'prefix',
     * result list is constrained by 'limit'.
     *
     * skip #REDIRECT
     * SELECT id,page_title,word_count,wiki_link_count,is_in_wiktionary FROM page WHERE page_title LIKE 'zzz%' AND is_redirect is NULL LIMIT 1;
     *
     * any entries, with #REDIRECT too
     * SELECT id,page_title,word_count,wiki_link_count,is_in_wiktionary,is_redirect,redirect_target FROM page WHERE page_title LIKE 'S%' LIMIT 1;
     *
     * skip empty articles, i.e. is_in_wiktionary==FALSE
     * SELECT id,page_title,word_count,wiki_link_count,is_in_wiktionary FROM page WHERE page_title LIKE 'zzz%' AND is_in_wiktionary=1 LIMIT 1;
     *
     * @param  limit    constraint of the number of rows returned,
     *                  if it has a negative value then the constraint is omitted
     * @param  prefix   the begining of the page_titles
     * @param  b_skip_redirects return articles without redirects if true
     * @param  b_meaning return articles with definitions
     * @param  b_sem_rel return articles with semantic relations
     * @param  str_source_lang pages filtering for words with these language
     *                          codes, e.g. "ru en fr"
     * @return null if page_title is absent
     */
    public static TPage[] getByPrefix ( SQLiteDatabase db,String prefix,
                                        int limit, boolean b_skip_redirects,
                                        TLang source_lang[], // String str_source_lang,
                                                                boolean b_meaning,
                                                                boolean b_sem_rel
                                       )
    {// todo: as func parameter ...
        boolean b_trans = true;

        /** target (translation) language which filters the words */
        // todo: TLang[] trans_lang;

        // TLang source_lang[] = new TLang[1];
        // source_lang[0] = TLang.get(LanguageType.en);

/** Language codes for words filtering, e.g. "ru en fr" */
//var lang_source_value: String = bind lang_source_Text.rawText;
// var source_lang : TLang[];
//TLang source_lang[] = TLang.parseLangCode(str_source_lang);

        TLang trans_lang[] = new TLang[0];
        //trans_lang[0] = TLang.get(LanguageType.fi);
        
        if(0==limit)
            return NULL_TPAGE_ARRAY;

        String str_limit = "";
        if(limit > 0) {
            int limit_with_reserve = limit;
            if(b_meaning)
                limit_with_reserve += 42; // since some words without meaning will be skipped
    
            if(b_sem_rel)
                limit_with_reserve += 512; // since some words without relations will be skipped
    
            if(source_lang.length > 0)
                limit_with_reserve += 555;
    
            if(trans_lang.length > 0)
                limit_with_reserve += 55555;
            str_limit = "" + limit_with_reserve;
        }

        // "page_title LIKE \"" + prefix + "\"" + str_skip_redirects
        StringBuilder s_where = new StringBuilder();
        if(prefix.length() > 0) {
            if(b_skip_redirects)
                s_where.append("page_title LIKE \"" + prefix + "%\" AND is_redirect is NULL");
            else
                s_where.append("page_title LIKE \"" + prefix + "%\"");
        } else {
            if(b_skip_redirects)
                s_where.append("is_redirect is NULL");
        }
        

        List<TPage> tp_list = null;
        // SELECT id,page_title,word_count,wiki_link_count,is_in_wiktionary,is_redirect,redirect_target FROM page WHERE page_title LIKE \"prefix\"
        Cursor c = db.query("page", 
                new String[] { "id", "page_title", "word_count", "wiki_link_count", 
                            "is_in_wiktionary", "is_redirect", "redirect_target"}, 
                s_where.toString(), 
                null, null, null, null,
                str_limit);
        
        if (c.moveToFirst()) {
            do {
                int i_id = c.getColumnIndexOrThrow("id");
                int i_page_title = c.getColumnIndexOrThrow("page_title");
                int i_word_count = c.getColumnIndexOrThrow("word_count");
                int i_wiki_link_count = c.getColumnIndexOrThrow("wiki_link_count");
                int i_is_in_wiktionary = c.getColumnIndexOrThrow("is_in_wiktionary");
                int i_is_redirect = c.getColumnIndexOrThrow("is_redirect");
                int i_redirect_target = c.getColumnIndexOrThrow("redirect_target");

                int _id = c.getInt(i_id);
                String _page_title = c.getString(i_page_title);
                int _word_count = c.getInt(i_word_count);
                int _wiki_link_count = c.getInt(i_wiki_link_count);
                boolean _is_in_wiktionary = 0 != c.getInt(i_is_in_wiktionary);
                boolean _is_redirect      = 0 != c.getInt(i_is_redirect);
                String _redirect_target = _is_redirect ? c.getString(i_redirect_target) : null;

                TPage tp = new TPage(_id, _page_title, _word_count, _wiki_link_count,
                                     _is_in_wiktionary, _redirect_target);

                tp.lang_pos = TLangPOS.getRecursive(db, tp);

                boolean b_add = true;
                if(b_meaning)
                    b_add = b_add && tp.hasDefinition();

                if(b_sem_rel)
                    b_add = b_add && tp.hasSemanticRelation();

                if(source_lang.length > 0)
                    b_add = b_add && tp.hasLanguage(source_lang);

                if(trans_lang.length > 0)
                    b_add = b_add && tp.hasTranslation(trans_lang);

                if(b_add) {
                    if(null == tp_list)
                        tp_list = new ArrayList<TPage>();

                    tp_list.add(tp);
                }

                // System.out.println(" title=" + page_title);
                //        "; redirect_target=" + redirect_target +
                //        "; id=" + id +
                //        "; is_redirect=" + is_redirect +
                //        " (TPage.getByPrefix)");

            } while (c.moveToNext() &&
                    (limit < 0 || null == tp_list || tp_list.size() < limit));
        }
        if (c != null && !c.isClosed()) {
            c.close();
        }
        if(null == tp_list)
            return NULL_TPAGE_ARRAY;

        return ((TPage[])tp_list.toArray(NULL_TPAGE_ARRAY));
    }

    /** Deletes row from the table 'page' by the page_title.
     *
     *  DELETE FROM page WHERE page_title="apple";
     *
     * @param  page_title  title of Wiktionary article
     */
    /*public static void delete (Connect connect,String page_title) {

        StringBuilder str_sql = new StringBuilder();
        try {
            Statement s = connect.conn.createStatement ();
            try {
                String safe_title = PageTableBase.convertToSafeStringEncodeToDBWunderscore(connect, page_title);

                str_sql.append("DELETE FROM page WHERE page_title=\"");
                str_sql.append(safe_title);
                str_sql.append("\"");
                s.execute (str_sql.toString());
                //System.out.println("TPage delete()):: page_title='" + page_title + "'");
            } finally {
                s.close();
            }
        } catch(SQLException ex) {
            System.err.println("SQLException (wikt_parsed TPage.java delete()):: sql='" + str_sql.toString() + "' " + ex.getMessage());
        }
    }*/


    /** Checks whether the article 'page_title' has any definitions. 
     * The field 'lang_pos' is scanned here.
     */
    public boolean hasDefinition() {

        if(null == lang_pos)
            return false;

        for(TLangPOS lp : lang_pos) {
            if(lp.getMeaning().length > 0)
                return true;
        }      
        return false;
    }
    
    /** Checks whether the article 'page_title' has at least one synonym, 
     * antonym, etc. The fields 'lang_pos', 'lang_pos.meaning' and
     * 'lang_pos.meaning.relation' are scanned here.
     */
    public boolean hasSemanticRelation() {

        if(null == lang_pos)
            return false;
        
        for(TLangPOS lp : lang_pos) {
            TMeaning[] mm = lp.getMeaning();
            for(TMeaning m : mm) {
                if(m.getRelation().size() > 0)
                    return true;
            }
        }
        return false;
    }

    /** Checks whether the article 'page_title' has at least one translatio
     * into the destination languages from the array 'trans_lang'.
     * The fields 'lang_pos', 'lang_pos.translation' are scanned here.
     */
    public boolean hasTranslation(TLang trans_lang[]) {

        if(null == lang_pos)
            return false;

        for(TLangPOS lp : lang_pos) {
            TMeaning[] mm = lp.getMeaning();
            for(TMeaning m : mm) {
                if(m.hasTranslation(trans_lang))
                    return true;
            }
        }
        return false;
    }

    /** Checks whether the article 'page_title' has at least one wordform
     * in the language from the array 'source_lang'.
     * The language-POS of this page_title is scanned here.
     */
    public boolean hasLanguage(TLang source_lang[]) {

        if(null == lang_pos)
            return false;

        for(TLangPOS lp : lang_pos) {

            TLang lang = lp.getLang();

            for(TLang source : source_lang)
                if(lang == source)
                    return true;
        }

        return false;
    }
    
    /** Gets array of titles of pages from TPage[] objects.
     * @return empty array if source array is empty.
     */
    public static String[] getPageTitles (TPage[] page_array) {
        
        if(null == page_array || page_array.length < 1)
            return NULL_STRING_ARRAY;
        
        String[] page_titles = new String[ page_array.length ];
        for(int i=0; i < page_array.length; i++)
            page_titles[i] = page_array[i].getPageTitle();
        
        return page_titles;
    }
}
