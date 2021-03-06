package org.appxi.cbeta.explorer.dao;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.Score;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SolrDocument(collection = "pieces")
public class Piece {

    @org.springframework.data.annotation.Id
    @Indexed("id")
    public String id;

    @Indexed("_provider_s")
    public String provider;

    @Indexed("_version_s")
    public String version;

    @Indexed("project_s")
    public String project;

    @Indexed("path_s")
    public String path;

    @Indexed("type_s")
    public String type;

    @Indexed("title_s")
    public String title;

    @Indexed("priority_d")
    public double priority = 5;

    @Field("category_ss")
    public List<String> categories;

    // <field name="'field_' + mapEntry[0].key">mapEntry[0].value</field>
    // <field name="'field_' + mapEntry[1].key">mapEntry[1].value</field>
    @Dynamic
    @Field("field_*")
    public Map<String, String> fields;

    // <field name="mapEntry[0].key">mapEntry[0].value</field>
    // <field name="mapEntry[1].key">mapEntry[1].value</field>
    @Field("content_*")
    public Map<String, String> contents;

    // <field name="'extra_' + mapEntry[0].key">mapEntry[0].value</field>
    // <field name="'extra_' + mapEntry[1].key">mapEntry[1].value</field>
    @Dynamic
    @Field("extra_*")
    public Map<String, Object> extras;

    @Score
    public double score;

    public static Piece of() {
        final Piece piece = new Piece();
        piece.categories = new ArrayList<>();
        piece.fields = new HashMap<>();
        piece.contents = new HashMap<>();
        piece.extras = new HashMap<>();
        return piece;
    }
}

