package com.facebook.LinkBench.neo4j;

import com.facebook.LinkBench.*;
import com.facebook.LinkBench.Node;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.IOException;
import java.util.*;

/**
 * @author mh
 * @since 04.04.13
 * remove visibility for invisible links
 */
public class Neo4jGraphStore extends GraphStore {
    private static final int BATCH_SIZE = 30000;
    GraphDatabaseService db;
    private Index<org.neo4j.graphdb.Node> nodeIndex;
    private UniqueFactory.UniqueNodeFactory uniqueNodeFactory;

    @Override
    public void initialize(Properties p, Phase currentPhase, int threadId) throws IOException, Exception {
        // ignore it if db already exists
        if (this.db!=null) return;
        final HashMap<String, String> config = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            config.put(entry.getKey().toString(), entry.getValue().toString());
        }
        this.db = new EmbeddedGraphDatabase(p.getProperty(Config.DBID), config);
        nodeIndex = db.index().forNodes("nodes");
        uniqueNodeFactory = new UniqueFactory.UniqueNodeFactory(nodeIndex) {
            @Override
            protected void initialize(org.neo4j.graphdb.Node created, Map<String, Object> properties) {
                created.setProperty("id", properties.get("id"));
            }
        };
    }

    public void resetNodeStore(String dbid, long startID) throws Exception {
    }

    public long addNode(String dbid, Node node) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            final long result = doAddNode(node);
            tx.success();
            return result;
        } finally {
            tx.finish();
        }
    }

    private long doAddNode(Node node) {
        final org.neo4j.graphdb.Node neoNode = obtainNode(node.id);
        neoNode.setProperty("data", node.data);
        neoNode.setProperty("time", node.time);
        neoNode.setProperty("version", node.version);
        neoNode.setProperty("type", node.type); // todo label
        return node.id;
    }

    public Node getNode(String dbid, int type, long id) throws Exception {
        final org.neo4j.graphdb.Node neoNode = getNodeById(id);
        return new Node(neoNode.getId(),
                (Integer) neoNode.getProperty("type"),
                (Long) neoNode.getProperty("version"),
                (Integer) neoNode.getProperty("time"),
                (byte[]) neoNode.getProperty("data"));
    }

    private org.neo4j.graphdb.Node getNodeById(long id) {
        return nodeIndex.get("id",id).getSingle();
        // return db.getNodeById(id);
    }

    public boolean updateNode(String dbid, Node node) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            final org.neo4j.graphdb.Node neoNode = getNodeById(node.id);
            neoNode.setProperty("data", node.data);
            neoNode.setProperty("time", node.time);
            neoNode.setProperty("version", node.version);
            neoNode.setProperty("type", node.type); // todo label
            tx.success();
            return true;
        } catch (NotFoundException nfe) {
            return false;
        } finally {
            tx.finish();
        }
    }

    public boolean deleteNode(String dbid, int type, long id) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            final org.neo4j.graphdb.Node neoNode = getNodeById(id);
            if (neoNode==null) return false;
            for (Relationship relationship : neoNode.getRelationships()) {
                relationship.delete();
            }
            neoNode.delete();
            tx.success();
            return true;
        } catch (NotFoundException nfe) {
            tx.failure();
            return false;
        } finally {
            tx.finish();
        }
    }

    @Override
    public void close() {
        db.shutdown();
        db=null;
    }

    @Override
    public void clearErrors(int threadID) {

    }


    @Override
    public boolean addLink(String dbid, Link link, boolean noinverse) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            final boolean result = doAddLink(link, noinverse);
            tx.success();
            return result;
        } catch (NotFoundException nfe) {
            tx.failure();
            return false;
        } finally {
            tx.finish();
        }
    }

    private org.neo4j.graphdb.Node obtainNode(long id) {
        return uniqueNodeFactory.getOrCreate("id", id);
    }

    private boolean doAddLink(Link link, boolean noinverse) {
        final long link_type = link.link_type;
        org.neo4j.graphdb.Node start = obtainNode(link.id1);
        org.neo4j.graphdb.Node end = obtainNode(link.id2);
        Relationship rel = findLink(link.id1, link.link_type, link.id2);
        if (rel == null) {
            rel = start.createRelationshipTo(end, relType(link_type));
            updateRel(rel, link, noinverse);
            return true;
        } else {
            updateRel(rel, link, noinverse);
            return false;
        }
    }

    private void updateRel(Relationship rel, Link link, boolean noinverse) {
        rel.setProperty("data", link.data);
        rel.setProperty("time", link.time);
        rel.setProperty("version", link.version);
        rel.setProperty("visibility", link.visibility);
        rel.setProperty("noinverse", noinverse);
    }

    // todo cache
    private DynamicRelationshipType relType(long link_type) {
        return DynamicRelationshipType.withName(String.valueOf(link_type));
    }

    private Relationship findLink(long id1, long link_type, long id2) {
        final org.neo4j.graphdb.Node start = getNodeById(id1);
        final org.neo4j.graphdb.Node end = getNodeById(id2);
        if (start==null || end==null) return null;
        for (Relationship rel : start.getRelationships(relType(link_type))) {
            if (rel.getOtherNode(start).equals(end)) {
                return rel;
            }
        }
        return null;
    }

    @Override
    public boolean deleteLink(String dbid, long id1, long link_type, long id2, boolean noinverse, boolean expunge) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            final org.neo4j.graphdb.Node start = getNodeById(id1);
            final org.neo4j.graphdb.Node end = getNodeById(id2);
            if (start==null || end==null) return false;
            for (Relationship rel : start.getRelationships(relType(link_type))) {
                if (rel.getOtherNode(start).equals(end)) {
                    if (expunge) rel.delete();
                    else rel.removeProperty("visibility");
                    tx.success();
                    return true;
                }
            }
            return false;
        } catch (NotFoundException nfe) {
            tx.failure();
            return false;
        } finally {
            tx.finish();
        }
    }

    @Override
    public boolean updateLink(String dbid, Link a, boolean noinverse) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            final Relationship rel = findLink(a.id1, a.link_type, a.id2);
            if (rel == null) return false;
            updateRel(rel, a, noinverse);
            tx.success();
            return true;
        } finally {
            tx.finish();
        }
    }

    @Override
    public Link getLink(String dbid, long id1, long link_type, long id2) throws Exception {
        final Relationship rel = findLink(id1, link_type, id2);
        if (rel==null) return null;
        return createLink(id1, link_type, id2, rel);
    }

    private Link createLink(long id1, long link_type, long id2, Relationship rel) {
        return new Link(id1, link_type, id2, (Byte) rel.getProperty("visibility", (byte)0), (byte[]) rel.getProperty("data"), (Integer) rel.getProperty("version",0), (Long) rel.getProperty("time",0));
    }

    @Override
    public Link[] getLinkList(String dbid, long id1, long link_type) throws Exception {
        try {
            List<Link> links = new ArrayList<Link>(100);
            final org.neo4j.graphdb.Node start = getNodeById(id1);
            if (start==null) return null;
            for (Relationship rel : start.getRelationships(relType(link_type), Direction.OUTGOING)) {
                if (!isVisible(rel)) continue;
                links.add(createLink(id1, link_type, (Long)rel.getOtherNode(start).getProperty("id"), rel));
            }
            return toSortedLinkArray(links);
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    @Override
    public Link[] getLinkList(String dbid, long id1, long link_type, long minTimestamp, long maxTimestamp, int offset, int limit) throws Exception {
        try {
            List<Link> links = new ArrayList<Link>(100);
            final org.neo4j.graphdb.Node start = getNodeById(id1);
            for (Relationship rel : start.getRelationships(relType(link_type), Direction.OUTGOING)) {
                long time = (Long) rel.getProperty("time");
                if (time < minTimestamp || time > maxTimestamp || !isVisible(rel)) continue;
                links.add(createLink(id1, link_type, (Long) rel.getEndNode().getProperty("id"), rel));
            }
            return toLimitedSortedLinkArray(links, offset, limit);
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    private Link[] toLimitedSortedLinkArray(List<Link> links, int offset, int limit) {
        if (links.isEmpty()) return null;
        Collections.sort(links, new LinkComparator());
        links = links.subList(offset, Math.min(offset + limit,links.size()));
        return links.toArray(new Link[links.size()]);
    }

    private Link[] toSortedLinkArray(List<Link> links) {
        if (links.isEmpty()) return null;
        final Link[] result = links.toArray(new Link[links.size()]);
        Arrays.sort(result, new LinkComparator());
        return result;
    }

    @Override
    public long countLinks(String dbid, long id1, long link_type) throws Exception {
        try {
            final org.neo4j.graphdb.Node node = getNodeById(id1);
            if (node==null) return -1;
            int count = 0;
            for (Relationship rel : node.getRelationships(relType(link_type),Direction.OUTGOING)) {
                if (isVisible(rel)) count++;
            }
            return count;
        } catch(NotFoundException nfe) {
            return -1;
        }
    }

    private boolean isVisible(Relationship rel) {
        return (Byte)rel.getProperty("visibility",(byte)0)!=0;
    }

    @Override
    public long[] bulkAddNodes(String dbid, List<Node> nodes) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            long ids[] = new long[nodes.size()];
            int i = 0;
            for (Node node : nodes) {
                long id = doAddNode(node);
                ids[i++] = id;
            }
            tx.success();
            return ids;
        } finally {
            tx.finish();
        }
    }

    @Override
    public void addBulkLinks(String dbid, List<Link> a, boolean noinverse) throws Exception {
        final Transaction tx = db.beginTx();
        try {
            for (Link link : a) {
                doAddLink(link, noinverse);
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Override
    public void addBulkCounts(String dbid, List<LinkCount> a) throws Exception {
        // don't know what to do with this super.addBulkCounts(dbid, a);
    }

    @Override
    public int bulkLoadBatchSize() {
        return BATCH_SIZE;
    }

    private static class LinkComparator implements Comparator<Link> {
        public int compare(Link o1, Link o2) {
            return o1.time > o2.time ? -1 : 1;
        }
    }
}
