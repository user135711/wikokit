/* TRelationType.java - SQL operations with the table 'relation_type'
 * in Wiktionary parsed database.
 *
 * Copyright (c) 2009 Andrew Krizhanovsky <andrew.krizhanovsky at gmail.com>
 * Distributed under GNU Public License.
 */

package wikt.sql;

import wikt.constant.Relation;

import wikipedia.sql.Connect;
import wikipedia.sql.UtilSQL;
import wikipedia.sql.Statistics;
import java.sql.*;

import java.util.Map;
import java.util.LinkedHashMap;
//import java.util.Set;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** An operations with the table 'relation_type' in Wiktionary parsed database.
 * The table 'relation_type' contains a list of semantic relations: name and ID.
 */
public class TRelationType {

    /** Unique POS identifier. */
    private int id;

    /** Name of semantic relations, e.g. synonymy. */
    private Relation name;

    /** Map from an ID to a relation. It is created from data of the table `relation_type`,
     * which is created from data in Relation.java.*/
    private static Map<Integer, TRelationType> id2relation;

    /** Map from a relation to an ID.*/
    private static Map<Relation, Integer> relation2id;

    private final static TRelationType[] NULL_TRELATIONTYPE_ARRAY = new TRelationType[0];

    public TRelationType(int _id,Relation _name) {
        id   = _id;
        name = _name;
    }

    /** Gets an unique ID of this relation. */
    public int getID() {
        return id;
    }

    /** Gets a semantic relation. */
    public Relation getRelation() {
        return name;
    }
    
    /** Gets semantic relations' ID from the table 'relation_type'.<br><br>
     * REM: createFastMaps() should be run at least once, before this function execution.
     */
    public static int getIDFast(Connect connect,Relation r) {
        if(null == relation2id) {
            System.err.println("Error (wikt_parsed TRelationType.getIDFast()):: What about calling 'createFastMaps()' before?");
            return -1;
        }
        return relation2id.get(r);
    }

    /** Gets semantic relations (name with ID) from the table 'relation_type'.<br><br>
     * REM: createFastMaps() should be run at least once, before this function execution.
     */
    public static TRelationType getRelationFast(Connect connect,Relation r) {
        if(null == id2relation) {
            System.err.println("Error (wikt_parsed TRelationType.getRelationFast()):: What about calling 'createFastMaps()' before?");
            return null;
        }
        return id2relation.get(relation2id.get(r));
    }

    /** Gets semantic relations by ID from the table 'relation_type'.<br><br>
     * REM: createFastMaps() should be run at least once, before this function execution.
     */
    public static TRelationType getRelationFast(Connect connect,int id) {
        return id2relation.get(id);
    }

    /** Read all records from the table 'relation_type',
     * fills the internal map from a table ID to a semantic relation .<br><br>
     *
     * REM: during a creation of Wiktionary parsed database
     * the functions recreateTable() should be called (before this function).
     */
    public static void createFastMaps(Connect connect) {

        System.out.println("Loading table `relation_type`...");

        TRelationType[] rel_type = getAllRelations(connect);
        int size = rel_type.length;
        if(rel_type.length != Relation.size())
            System.out.println("Warning (wikt_parsed TRelationType.java createFastMaps()):: Relation.size (" + Relation.size()
                    + ") is not equal to size of table 'relation_type'("+ size +"). Is the database outdated?");

        if(null != id2relation && id2relation.size() > 0)
            id2relation.clear();
        if(null != relation2id && relation2id.size() > 0)
            relation2id.clear();

        id2relation  = new LinkedHashMap<Integer, TRelationType>(size);
        relation2id  = new LinkedHashMap<Relation, Integer>(size);

        for(TRelationType t : rel_type) {
            id2relation.put(t.getID(), t);
            relation2id.put(t.getRelation(), t.getID());
        }
    }

    /** Gets all records from the table 'relation_type'.
     */
    private static TRelationType[] getAllRelations(Connect connect) {

        int size = Statistics.Count(connect, "relation_type");
        if(0==size) {
            System.err.println("Error (wikt_parsed TRelationType.java getAllRelations()):: The table `relation_type` is empty!");
            return NULL_TRELATIONTYPE_ARRAY;
        }

        List<TRelationType>list_rel = new ArrayList<TRelationType>(size);

        Collection<Relation> rr = Relation.getAllRelations();
        for(Relation r : rr) {
            TRelationType t = get(connect, r);
            if(null != t)
                list_rel.add(t);
        }
        return( (TRelationType[])list_rel.toArray(NULL_TRELATIONTYPE_ARRAY) );
    }


    /** Deletes all records from the table 'relation_type',
     * loads parts of speech names from POS.java,
     * sorts by name,
     * fills the table.
     */
    public static void recreateTable(Connect connect) {

        System.out.println("Recreating the table `relation_type`...");
        Map<Integer, Relation> _id2relation = fillLocalMaps();
        UtilSQL.deleteAllRecordsResetAutoIncrement(connect, "relation_type");
        fillDB(connect, _id2relation);
        {
            int db_current_size = wikipedia.sql.Statistics.Count(connect, "relation_type");
            assert(db_current_size == Relation.size()); // ~ 9 POS
        }
    }

    /** Load data from a Relation class, sorts,
     * and fills the local map 'id2relation'. */
    public static Map<Integer, Relation> fillLocalMaps() {

        int size = Relation.size();
        List<String>list_rel = new ArrayList<String>(size);
        list_rel.addAll(Relation.getAllRelationNames());
        Collections.sort(list_rel);             // Collections.sort(list_rel, StringUtil.LEXICOGRAPHICAL_ORDER);

        // OK, we have list of relation names. Sorted list 'list_rel'

        // Local map from id to Relation. It is created from data in Relation.java.
        // It is used to fill the table 'relation_type' in right sequence.
        Map<Integer, Relation> _id2relation = new LinkedHashMap<Integer, Relation>(size);
        for(int id=0; id<size; id++) {
            String s = list_rel.get(id);    // s - semantic relation name
            assert(Relation.has(s));                                                 //System.out.println("fillLocalMaps---id="+id+"; s="+s);
            _id2relation.put(id, Relation.get(s));
        }
        return _id2relation;
    }

    /** Fills database table 'relation_type' by data from Relation class. */
    public static void fillDB(Connect connect,Map<Integer, Relation> id2relation) {

        for(int id : id2relation.keySet())
            insert (connect, id2relation.get(id));
    }
    
    /** Inserts record into the table 'relation_type'.<br><br>
     * INSERT INTO relation_type (name) VALUES ("synonymy");
     * @param name  semantic relation, e.g. 'synonymy'
     */
    public static void insert (Connect connect,Relation r) {

        if(null == r) return;

        Statement   s = null;
        ResultSet   rs= null;
        StringBuffer str_sql = new StringBuffer();
        try
        {
            s = connect.conn.createStatement ();
            str_sql.append("INSERT INTO relation_type (name) VALUES (\"");
            //String safe_title = StringUtil.spaceToUnderscore(
            //                    StringUtil.escapeChars(name));
            //str_sql.append(safe_title);
            str_sql.append(r.toString());
            str_sql.append("\")");

            s.executeUpdate (str_sql.toString());
        }catch(SQLException ex) {
            System.err.println("SQLException (wikt_parsed TRelationType.java insert()):: sql='" + str_sql.toString() + "' " + ex.getMessage());
        } finally {
            if (rs != null) {   try { rs.close(); } catch (SQLException sqlEx) { }  rs = null; }
            if (s != null)  {   try { s.close();  } catch (SQLException sqlEx) { }  s = null;  }
        }
    }

    /** Selects row from the table 'relation_type' by a semantic relation name.<br><br>
     *  SELECT id FROM relation_type WHERE name="synonymy";
     * @param  r    semantic relation
     * @return null if a semantic relation is absent in the table 'relation_type'
     */
    public static TRelationType get (Connect connect,Relation r) {

        if(null == r) return null;

        Statement   s = null;
        ResultSet   rs= null;
        StringBuffer str_sql = new StringBuffer();
        TRelationType rel_type = null;
        try {
            s = connect.conn.createStatement ();
            str_sql.append("SELECT id FROM relation_type WHERE name=\"");
            str_sql.append(r.toString());
            str_sql.append("\"");
            s.executeQuery (str_sql.toString());
            rs = s.getResultSet ();
            if (rs.next ())
                rel_type = new TRelationType(rs.getInt("id"), r);
            else
                System.err.println("Warning: (wikt_parsed TRelationType.java get()):: POS (" + r.toString() + ") is absent in the table 'relation_type'.");
        } catch(SQLException ex) {
            System.err.println("SQLException (wikt_parsed TRelationType.java get()):: sql='" + str_sql.toString() + "' " + ex.getMessage());
        } finally {
            if (rs != null) {   try { rs.close(); } catch (SQLException sqlEx) { }  rs = null; }
            if (s != null)  {   try { s.close();  } catch (SQLException sqlEx) { }  s = null;  }
        }
        return rel_type;
    }

    /** Deletes row from the table 'relation_type' by the semantic relation name.<br><br>
     *  DELETE FROM relation_type WHERE name="synonymy";
     * @param  r    semantic relation to be deleted
     */
    public static void delete (Connect connect,Relation r) {

        if(null == r) return;

        Statement   s = null;
        ResultSet   rs= null;
        StringBuffer str_sql = new StringBuffer();
        try {
            s = connect.conn.createStatement ();
            str_sql.append("DELETE FROM relation_type WHERE name=\"");
            str_sql.append(r.toString());
            str_sql.append("\"");
            s.execute (str_sql.toString());
        } catch(SQLException ex) {
            System.err.println("SQLException (wikt_parsed TRelationType.java delete()):: sql='" + str_sql.toString() + "' " + ex.getMessage());
        } finally {
            if (rs != null) {   try { rs.close(); } catch (SQLException sqlEx) { }  rs = null; }
            if (s != null)  {   try { s.close();  } catch (SQLException sqlEx) { }  s = null;  }
        }
    }

}
