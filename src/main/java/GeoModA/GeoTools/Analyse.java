/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
package GeoModA.GeoTools;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.collection.SpatialIndexFeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;

public class Analyse {
	// ## Semantische Harmonisierung: Ausgabe einer neuen Shapedatei pro Attributklasse 
	static void createFilteredShape(SimpleFeatureSource fs, String toFilter, String attribute, String path, Boolean isAtkis) throws Exception {
		
		SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
		
		if (isAtkis)
		{
			stb.setName("ATKIS");
		    stb.add("the_geom", fs.getSchema().getType("the_geom").getBinding());
		    stb.add("OBJID", fs.getSchema().getType("OBJID").getBinding());
		    stb.add("OBJART", fs.getSchema().getType("OBJART").getBinding());
		    stb.add("OBJART_TXT", fs.getSchema().getType("OBJART_TXT").getBinding());
		    stb.add("BEGINN", fs.getSchema().getType("BEGINN").getBinding());
		    stb.add("ENDE", fs.getSchema().getType("ENDE").getBinding());
		    stb.add("NAM", fs.getSchema().getType("NAM").getBinding());
		}
		else
		{
			SimpleFeatureType sft_old = fs.getSchema();
		    stb.init(sft_old);
		    stb.setName("OSM");
		}

	    stb.add("Objektart", String.class);
	    SimpleFeatureType sft = stb.buildFeatureType();
	    SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(sft);
	    
	    SimpleFeatureIterator it = fs.getFeatures(CQL.toFilter(toFilter)).features();
	    DefaultFeatureCollection collection = new DefaultFeatureCollection();
	    if (isAtkis)
	    {
	    	try {
		        while (it.hasNext()) {
		            SimpleFeature sf = it.next();
		            sfb.add(String.valueOf(sf.getAttribute("the_geom")));
		            sfb.add(String.valueOf(sf.getAttribute("OBJID").toString()));
		            sfb.add(String.valueOf(sf.getAttribute("OBJART").toString()));
		            sfb.add(String.valueOf(sf.getAttribute("OBJART_TXT").toString()));
		            sfb.add(String.valueOf(sf.getAttribute("BEGINN").toString()));
		            sfb.add(String.valueOf(sf.getAttribute("ENDE")).toString());
		            sfb.add(String.valueOf(sf.getAttribute("NAM")).toString());
		            sfb.add(String.valueOf(attribute));
		            collection.add(sfb.buildFeature(null));
		        }
		    } finally {
		        it.close();
		    }
	    }
	    else
	    {
	    	try {
		        while (it.hasNext()) {
		            SimpleFeature sf = it.next();
		            sfb.addAll(sf.getAttributes());
		            sfb.add(String.valueOf(attribute));
		            collection.add(sfb.buildFeature(null));
		        }
		    } finally {
		        it.close();
		    }
	    }    
	    
	    
	    File file = new File(path);
	
	    ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
	
	    Map<String, Serializable> params = new HashMap<String, Serializable>();
	    params.put("url", file.toURI().toURL());
	    params.put("create spatial index", Boolean.TRUE);
	
	    ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
	    newDataStore.createSchema(sft);
	
	    Transaction transaction = new DefaultTransaction("create");
	
	    String typeName = newDataStore.getTypeNames()[0];
	    SimpleFeatureSource newFeatureSource = newDataStore.getFeatureSource(typeName);
	
	    if (newFeatureSource instanceof SimpleFeatureStore) {
	        SimpleFeatureStore featureStore = (SimpleFeatureStore) newFeatureSource;
	
	        featureStore.setTransaction(transaction);
	        try {
	            featureStore.addFeatures(collection);
	            transaction.commit();
	
	        } catch (Exception problem) {
	            problem.printStackTrace();
	            transaction.rollback();
	
	        } finally {
	            transaction.close();
	        }
	    } else {
	        System.out.println(typeName + " does not support read/write access");
	        System.exit(1);
	    }
	}
	
	// ## Schwerpunktreduzierung ##
	static void convertAreaToPoint(SimpleFeatureSource fs_area, SimpleFeatureSource fs_p) throws Exception {
		// Konvertierung OSM: MultiLineString zu (Schwer)Punkt & Speichern in Punktshape
		// Konvertierung ATKIS: MultiPolygon zu (Schwer)Punkt & Speichern in Punktshape
        SimpleFeatureStore store = (SimpleFeatureStore) fs_p;
        SimpleFeatureBuilder build = new SimpleFeatureBuilder(store.getSchema());
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        int n = 0;
        
        List<SimpleFeature> list = new ArrayList<>();
        SimpleFeatureCollection fc = fs_area.getFeatures();
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature sf = features.next();
            	Polygon polygon = null;
            	
            	Geometry defaultGeom = (Geometry) sf.getDefaultGeometry();
            	if (defaultGeom.isSimple()) {
	            	if (defaultGeom instanceof MultiPolygon) {
	            		polygon = (Polygon) defaultGeom.getGeometryN(0);
	            	} else if (defaultGeom instanceof MultiLineString) {
		            	LineString line = (LineString) defaultGeom.getGeometryN(0);
		            	if (line.isClosed()) {
		            		LinearRing ring = geometryFactory.createLinearRing(line.getCoordinateSequence());
			            	polygon = geometryFactory.createPolygon(ring);
		            	}
	            	}
            	}
            	
            	if (polygon != null) {
	            	Object obj[] = new Object[sf.getAttributeCount()];
	            	obj[0] = polygon.getCentroid();
	            	for (int i=1;i<obj.length;i++) {
	            		obj[i] = sf.getAttribute(i);
	            	}
	            	list.add(build.buildFeature(null, obj));
	            	n++;
            	}
            }
        }
        SimpleFeatureCollection collection = new ListFeatureCollection(store.getSchema(), list);

        Transaction transaction = new DefaultTransaction("Add Features");
        store.setTransaction(transaction);
        try {
            store.addFeatures(collection);
            transaction.commit();
        } catch (Exception eek) {
            transaction.rollback();
        }
        System.out.print("Anzahl der konvertierten Objekte von " + fs_area.getSchema().getName() + " nach " + fs_p.getSchema().getName() + ": " + n + "\n");
	}
	
	// ## Filterung von Punkte, die sich auf das gleiche Real-Objekt beziehen ##
	static void removeDuplicatePoints(SimpleFeatureSource fs, double bufferRadius) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures();
        try (SimpleFeatureIterator features = fc.features()) {
        	int n = 0;
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	Geometry buffer = geom.buffer(bufferRadius);
                try (SimpleFeatureIterator features2 = fc.features()) {
                    while (features2.hasNext()) {
                    	SimpleFeature feature2 = features2.next();
                    	if (!feature.getID().toString().equals(feature2.getID().toString())) {
                    		Geometry geom2 = (Geometry) feature2.getDefaultGeometry();
                    		if (buffer.intersects(geom2)) {
                    			SimpleFeatureStore store = (SimpleFeatureStore) fs;
                    	        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
                    	        Filter filter = ff.id(Collections.singleton(ff.featureId(feature2.getID())));
                    			store.removeFeatures(filter);
                    			n++;
                    		}
                    	}
                  	}
                }
          	}
            System.out.print("Gelöschte (doppelte) Punkte in " + fs.getSchema().getName() + ": " + n + "\n");
        }
	}
	
	// ## Punktzuordnung: Buffern von fs1, Verschneidung mit Punktdatensatz fs2 ##
	static void positionPointToPoint(SimpleFeatureSource fs1, SimpleFeatureSource fs2, double bufferRadius) throws Exception {
		SimpleFeatureCollection fc = fs1.getFeatures();
		Polygon[] polygons = new Polygon[fc.size()];
		int i = 0;
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	Polygon buffer = (Polygon) geom.buffer(bufferRadius);
            	polygons[i] = buffer;
            	i++;
          	}
        }
		Collection<Geometry> collection = Arrays.asList(polygons);
        UnaryUnionOp geomUnion = new UnaryUnionOp(collection);
        Geometry combined = geomUnion.union();

		SimpleFeatureCollection fc2 = fs2.getFeatures();
		
        int n = 0;
        try (SimpleFeatureIterator features = fc2.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
        		if (geom.intersects(combined)) {
        			n++;
        		}
          	}
        }
        System.out.print("Passende Punkte bei " + bufferRadius + " m Radius zwischen " + fs1.getSchema().getName() + " und " + fs2.getSchema().getName() + ": " + n + "\n");
	}
		
	// ## Rückgabe aller gebufferten Einzelgeometrien einer FS in einer einzelnen Geometrie ##
	static Geometry bufferedUnionGeometry(SimpleFeatureSource fs, double bufferRadius) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		int size = fc.size();
		Polygon[] polygons = new Polygon[size];
		int i = 0;
		int nonvalid = 0;
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	if (geom.isSimple() && geom.isValid()) {
            		Polygon polygon = null;
	            	if (geom instanceof MultiPolygon) {
	            		polygon = (Polygon) geom.getGeometryN(0);
	            		if (bufferRadius != 0.0) {polygon = (Polygon) polygon.buffer(bufferRadius);}
	            	} 
	            	else if (geom instanceof MultiLineString) {
		            	LineString line = (LineString) geom.getGeometryN(0);
		            	if (line.isClosed()) {
		            		LinearRing ring = geometryFactory.createLinearRing(line.getCoordinateSequence());
			            	polygon = geometryFactory.createPolygon(ring);
			            	if (bufferRadius != 0.0) {polygon = (Polygon) polygon.buffer(bufferRadius);}
		            	}
	            	}
	            	else if (geom instanceof Point) {
	            		if (bufferRadius != 0.0) {polygon = (Polygon) geom.buffer(bufferRadius);}
	            	}
            		
	            	if (polygon!=null) {
	                	polygons[i] = polygon;
	                	i++;
	            	}
            	}
            	else 
            	{
            		++nonvalid;
            	}
          	}
        }
		Collection<Geometry> collection = Arrays.asList(Arrays.copyOfRange(polygons, 0, size-nonvalid));
        UnaryUnionOp geomUnion = new UnaryUnionOp(collection);
        Geometry combined = geomUnion.union();
        return combined;
	}
	static Geometry bufferedUnionGeometry(SimpleFeatureSource fs, double bufferRadius, String toFilter) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures(CQL.toFilter(toFilter));
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		int size = fc.size();
		Polygon[] polygons = new Polygon[size];
		int i = 0;
		int nonvalid = 0;
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	if (geom.isSimple() && geom.isValid()) {
            		Polygon polygon = null;
	            	if (geom instanceof MultiPolygon) {
	            		polygon = (Polygon) geom.getGeometryN(0);
	            		polygon = (Polygon) polygon.buffer(bufferRadius);
	            	} 
	            	else if (geom instanceof MultiLineString) {
		            	LineString line = (LineString) geom.getGeometryN(0);
		            	if (line.isClosed()) {
		            		LinearRing ring = geometryFactory.createLinearRing(line.getCoordinateSequence());
			            	polygon = geometryFactory.createPolygon(ring);
			            	polygon = (Polygon) polygon.buffer(bufferRadius);
		            	}
	            	}
	            	else if (geom instanceof Point) {
	            		polygon = (Polygon) geom.buffer(bufferRadius);
	            	}
            		
	            	if (polygon!=null) {
	                	polygons[i] = polygon;
	                	i++;
	            	}
            	}
            	else
            	{
            		++nonvalid;
            	}
          	}
        }
		Collection<Geometry> collection = Arrays.asList(Arrays.copyOfRange(polygons, 0, size-nonvalid));
        UnaryUnionOp geomUnion = new UnaryUnionOp(collection);
        Geometry combined = geomUnion.union();
        return combined;
	}
	
	// ## Rückgabe der Gesamtsumme aller Einzelflächen einer FeatureSource ##
	static double calculateArea(SimpleFeatureSource fs) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures();
		double area = 0;
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	if (geom.isSimple() && geom.isValid()) {
	            	if (geom instanceof MultiPolygon || geom instanceof MultiLineString) {
	            		area += geom.getGeometryN(0).getArea();
	            	}
            	}
          	}
        }
        return area;
	}
	
	// ## Topologische Konsistenz ##
	static void topologicalConsistency(SimpleFeatureSource fs) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures();
		int validFeatures = 0;
		int simpleFeatures = 0;
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	if (!geom.isValid())  {validFeatures++;}
            	if (!geom.isSimple()) {simpleFeatures++;}
          	}	
        }
        System.out.print("Objektanzahl in " + fs.getSchema().getName() + ": " + fc.size() + "\n");
        System.out.print("Nichtvalide Objekte in " + fs.getSchema().getName() + ": " + validFeatures + "\n");
        System.out.print("Nicht-Simple Features in " + fs.getSchema().getName() + ": " + simpleFeatures + "\n");
	}
	static void printNotSimpleFeatures(SimpleFeatureSource fs) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures();
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	if (!geom.isSimple()) {
	            	System.out.print(geom.getNumGeometries() + " ");
	            	if (geom instanceof MultiLineString) {
		            	MultiLineString line = (MultiLineString) geom;
		            	System.out.print(line.isClosed() + " ");
	            	}
	            	for (int i=1;i<feature.getAttributeCount();i++) {
	            		if (!feature.getAttribute(i).equals("")) {
	            			System.out.print(fc.getSchema().getDescriptor(i).getName() + "=" + feature.getAttribute(i) + "  ");
	            		}
	            	}
	            	System.out.print("\n");
            	}
          	}
        }
	}
	static void printInvalidFeatures(SimpleFeatureSource fs) throws Exception {
		SimpleFeatureCollection fc = fs.getFeatures();
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	Geometry geom = (Geometry) feature.getDefaultGeometry();
            	if (!geom.isValid()) {
	            	System.out.print(geom.getNumGeometries() + " ");
	            	if (geom instanceof MultiLineString) {
		            	MultiLineString line = (MultiLineString) geom;
		            	System.out.print(line.isClosed() + " ");
	            	}
	            	for (int i=1;i<feature.getAttributeCount();i++) {
	            		if (!feature.getAttribute(i).equals("")) {
	            			System.out.print(fc.getSchema().getDescriptor(i).getName() + "=" + feature.getAttribute(i) + "  ");
	            		}
	            	}
	            	System.out.print("\n");
            	}
          	}
        }
	}
	
	// ## Schreibt eine Liste an Attributen einer FeatureSource in eine Textdatei 
	static void writeObjectlist(SimpleFeatureSource fs, String writerPath, String[] writerAttributes) throws Exception {
		PrintWriter writer = new PrintWriter(writerPath, "UTF-8");
        SimpleFeatureCollection fc = fs.getFeatures();
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	writer.print(feature.getID());
                writer.print(":");
            	for(String elem: writerAttributes) {
            		writer.print("   " + elem + "=");
	                writer.print(feature.getAttribute(elem));
            	}
                writer.print("\n");
            }
            writer.close();
        }
	}
	static void writeObjectlist(SimpleFeatureSource fs, String toFilter, String writerPath, String[] writerAttributes) throws Exception {
		PrintWriter writer = new PrintWriter(writerPath, "UTF-8");
        Filter filter = CQL.toFilter(toFilter);
        SimpleFeatureCollection fc = fs.getFeatures(filter);
        try (SimpleFeatureIterator features = fc.features()) {
            while (features.hasNext()) {
            	SimpleFeature feature = features.next();
            	writer.print(feature.getID());
                writer.print(":");
            	for(String elem: writerAttributes) {
            		writer.print("   " + elem + "=");
	                writer.print(feature.getAttribute(elem));
            	}
                writer.print("\n");
            }
            writer.close();
        }
	}
	
	public static void main(String[] args) throws Exception {
    	String path ="D:/Hochschule/Master/GeoModA/shapefiles/";
        String osmFilter = "";
        String atkisFilter = "";
        final DecimalFormat df = new DecimalFormat("#0.0000");
    	
    	//###########################
        //##  Alle Featuresources  ##
        //###########################
    	SimpleFeatureSource fs_sie01_f = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie01_f.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie02_f = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie02_f.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie03_f = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie03_f.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie03_l = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie03_l.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie03_p = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie03_p.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie04_f = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie04_f.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie04_l = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie04_l.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie05_f = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie05_f.shp")).getFeatureSource();
        SimpleFeatureSource fs_sie05_p = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/sie05_p.shp")).getFeatureSource();
        SimpleFeatureSource fs_atkis_gebaeude_f = FileDataStoreFinder.getDataStore(new File(path+"ATKIS/gebaeude.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_p = FileDataStoreFinder.getDataStore(new File(path+"OSM/OSM_point.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_l = FileDataStoreFinder.getDataStore(new File(path+"OSM/OSM_line.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_f = FileDataStoreFinder.getDataStore(new File(path+"OSM/OSM_polygon.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_landuse_p = FileDataStoreFinder.getDataStore(new File(path+"OSM/landuse/OSM_point_landuse.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_landuse_l = FileDataStoreFinder.getDataStore(new File(path+"OSM/landuse/OSM_line_landuse.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_landuse_f = FileDataStoreFinder.getDataStore(new File(path+"OSM/landuse/OSM_polygon_landuse.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_gebaeude_p = FileDataStoreFinder.getDataStore(new File(path+"OSM/building/OSM_point_building.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_gebaeude_l = FileDataStoreFinder.getDataStore(new File(path+"OSM/building/OSM_line_building.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_gebaeude_f = FileDataStoreFinder.getDataStore(new File(path+"OSM/building/OSM_polygon_building.shp")).getFeatureSource();

        //############################
        //##  Anzahl Gesamtobjekte  ##
        //############################
        System.out.println("Punkte: " + fs_osm_p.getFeatures().size());
        System.out.println("Linien: " + fs_osm_l.getFeatures().size());
        System.out.println("Flächen: " + fs_osm_f.getFeatures().size());
        
        //############################################
        //##  Prüfung der topologischen Konsistenz  ##
        //############################################
        topologicalConsistency(fs_osm_p);
        topologicalConsistency(fs_osm_l);
        topologicalConsistency(fs_osm_f);
        printNotSimpleFeatures(fs_osm_l);
        printInvalidFeatures(fs_osm_f);
        
        //###############
        //##  AX_Turm  ##
        //###############
        atkisFilter = "OBJART_TXT='AX_Turm'";
        osmFilter = "man_made='tower' or man_made='water_tower' or man_made='communications_tower' or man_made='chimney' or building='transformer_tower' or historic='tower'";
        
        createFilteredShape(fs_sie05_p, atkisFilter, "AX_Turm", path+"temp/AX_Turm_P.shp", true);
        createFilteredShape(fs_sie05_f, atkisFilter, "AX_Turm", path+"temp/AX_Turm_F.shp", true);
        createFilteredShape(fs_osm_p, osmFilter, "Turm", path+"temp/Turm_P.shp", false);
        createFilteredShape(fs_osm_l, osmFilter, "Turm", path+"temp/Turm_L.shp", false);
        createFilteredShape(fs_osm_f, osmFilter, "Turm", path+"temp/Turm_F.shp", false);

        SimpleFeatureSource fs_atkis_turm_p = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Turm_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_atkis_turm_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Turm_F.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_turm_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Turm_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_turm_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Turm_L.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_turm_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Turm_F.shp")).getFeatureSource();
        
        //**Konvertierung ATKIS & OSM: Flächen zu Schwerpunkt**
        convertAreaToPoint(fs_atkis_turm_f, fs_atkis_turm_p);
        convertAreaToPoint(fs_osm_turm_l, fs_osm_turm_p);
        convertAreaToPoint(fs_osm_turm_f, fs_osm_turm_p);
        
        //Punkte aus dem gleichen Datensatz in Buffer löschen
        removeDuplicatePoints(fs_atkis_turm_p, 3.0); // Kontrolle, dass 3m in ATKIS noch keine Punkte fehlerhaft löscht
        removeDuplicatePoints(fs_osm_turm_p, 1.0); // 18
        removeDuplicatePoints(fs_osm_turm_p, 3.0); // 7
        
        System.out.print("Anzahl Turm-Objekt ATKIS: " + fs_atkis_turm_p.getFeatures().size() + "\n");
        System.out.print("Anzahl Turm-Objekt OSM: " + fs_osm_turm_p.getFeatures().size() + "\n");
        
        positionPointToPoint(fs_atkis_turm_p, fs_osm_turm_p, 1.0); //17
        positionPointToPoint(fs_atkis_turm_p, fs_osm_turm_p, 2.0); //39
        positionPointToPoint(fs_atkis_turm_p, fs_osm_turm_p, 5.0); //87
        positionPointToPoint(fs_atkis_turm_p, fs_osm_turm_p,10.0); //127
        //####################
        //##  Ende AX_Turm  ##
        //####################
        
        
        //########################################
        //## AX_VorratsbehaelterSpeicherbauwerk ##
        //########################################
        atkisFilter = "OBJART_TXT='AX_VorratsbehaelterSpeicherbauwerk'";
        osmFilter = "man_made='silo' or man_made='storage_tank' or man_made='gasometer' or man_made='bunker_silo'";

        createFilteredShape(fs_sie03_p, atkisFilter, "AX_Vorratsbehaelter", path+"temp/AX_Vorratsbehaelter_P.shp", true);
        createFilteredShape(fs_sie03_f, atkisFilter, "AX_Vorratsbehaelter", path+"temp/AX_Vorratsbehaelter_F.shp", true);
        createFilteredShape(fs_osm_p, osmFilter, "Vorratsbehaelter", path+"temp/Vorratsbehaelter_P.shp", false);
        createFilteredShape(fs_osm_l, osmFilter, "Vorratsbehaelter", path+"temp/Vorratsbehaelter_L.shp", false);
        createFilteredShape(fs_osm_f, osmFilter, "Vorratsbehaelter", path+"temp/Vorratsbehaelter_F.shp", false);

        SimpleFeatureSource fs_atkis_silo_p = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Vorratsbehaelter_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_atkis_silo_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Vorratsbehaelter_F.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_silo_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Vorratsbehaelter_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_silo_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Vorratsbehaelter_L.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_silo_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Vorratsbehaelter_F.shp")).getFeatureSource();
        
        //**Konvertierung ATKIS & OSM: Flächen zu Schwerpunkt**
        convertAreaToPoint(fs_atkis_silo_f, fs_atkis_silo_p);
        convertAreaToPoint(fs_osm_silo_l, fs_osm_silo_p);
        convertAreaToPoint(fs_osm_silo_f, fs_osm_silo_p);
        
        //Punkte aus dem gleichen Datensatz in Buffer löschen
        removeDuplicatePoints(fs_atkis_silo_p, 1.0); // 0
        removeDuplicatePoints(fs_atkis_silo_p, 2.0); // 0
        //removeDuplicatePoints(fs_atkis_silo_p, 3.0); // 3m würde einen ATKIS-Punkt löschen, deshalb bei OSM ebenfalls < 3m
        removeDuplicatePoints(fs_osm_silo_p, 0.1); // 0
        removeDuplicatePoints(fs_osm_silo_p, 0.5); // 0
        removeDuplicatePoints(fs_osm_silo_p, 1.0); // 0
        removeDuplicatePoints(fs_osm_silo_p, 2.0); // 0
        
        System.out.print("Anzahl Vorratbehaelter-Objekt ATKIS: " + fs_atkis_silo_p.getFeatures().size() + "\n");
        System.out.print("Anzahl Vorratbehaelter-Objekt OSM: " + fs_osm_silo_p.getFeatures().size() + "\n");
        
        positionPointToPoint(fs_atkis_silo_p, fs_osm_silo_p, 1.0);
        positionPointToPoint(fs_atkis_silo_p, fs_osm_silo_p, 2.0);
        positionPointToPoint(fs_atkis_silo_p, fs_osm_silo_p, 5.0);
        positionPointToPoint(fs_atkis_silo_p, fs_osm_silo_p,10.0);
        //#############################################
        //## Ende AX_VorratsbehaelterSpeicherbauwerk ##
        //#############################################
        
        
        //######################################################
        //## AX_HistorischesBauwerkOderHistorischeEinrichtung ##
        //######################################################
        atkisFilter = "OBJART_TXT='AX_HistorischesBauwerkOderHistorischeEinrichtung'";
        osmFilter = "historic NOT like '' and historic NOT like 'tower' and historic NOT like 'monument'";

        createFilteredShape(fs_sie03_p, atkisFilter, "AX_Historisch", path+"temp/AX_Historisch_P.shp", true);
        createFilteredShape(fs_sie03_f, atkisFilter, "AX_Historisch", path+"temp/AX_Historisch_F.shp", true);
        createFilteredShape(fs_osm_p, osmFilter, "Historisch", path+"temp/Historisch_P.shp", false);
        createFilteredShape(fs_osm_l, osmFilter, "Historisch", path+"temp/Historisch_L.shp", false);
        createFilteredShape(fs_osm_f, osmFilter, "Historisch", path+"temp/Historisch_F.shp", false);

        SimpleFeatureSource fs_atkis_hist_p = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Historisch_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_atkis_hist_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Historisch_F.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_hist_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Historisch_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_hist_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Historisch_L.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_hist_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Historisch_F.shp")).getFeatureSource();

        System.out.print("Anzahl Historisch-Objekt ATKIS: " + fs_atkis_hist_p.getFeatures().size() + "\n");
        System.out.print("Anzahl Historisch-Objekt OSM: " + fs_osm_hist_p.getFeatures().size() + "\n");
        
        //**Konvertierung ATKIS & OSM: Flächen zu Schwerpunkt**
        convertAreaToPoint(fs_atkis_hist_f, fs_atkis_hist_p);
        convertAreaToPoint(fs_osm_hist_l, fs_osm_hist_p);
        convertAreaToPoint(fs_osm_hist_f, fs_osm_hist_p);
        
        //Punkte aus dem gleichen Datensatz in Buffer löschen
        removeDuplicatePoints(fs_atkis_hist_p, 5.0); // Kontrolle: Keine gelöschten ATKIS-Punkte
        removeDuplicatePoints(fs_osm_hist_p, 0.5); // 
        removeDuplicatePoints(fs_osm_hist_p, 3.0); // 
        removeDuplicatePoints(fs_osm_hist_p, 5.0); // 
        
        System.out.print("Anzahl Historisch-Objekt ATKIS: " + fs_atkis_hist_p.getFeatures().size() + "\n");
        System.out.print("Anzahl Historisch-Objekt OSM: " + fs_osm_hist_p.getFeatures().size() + "\n");
        
        positionPointToPoint(fs_atkis_hist_p, fs_osm_hist_p, 1.0);
        positionPointToPoint(fs_atkis_hist_p, fs_osm_hist_p, 2.0);
        positionPointToPoint(fs_atkis_hist_p, fs_osm_hist_p, 5.0);
        positionPointToPoint(fs_atkis_hist_p, fs_osm_hist_p,10.0);
        positionPointToPoint(fs_atkis_hist_p, fs_osm_hist_p,20.0);
        //###########################################################
        //## Ende AX_HistorischesBauwerkOderHistorischeEinrichtung ##
        //###########################################################
        
        
        //#######################################################
        //## AX_SonstigesBauwerkOderSonstigeEinrichtung Punkte ##
        //#######################################################
        atkisFilter = "OBJART_TXT='AX_SonstigesBauwerkOderSonstigeEinrichtung'";
        osmFilter = "historic='monument' or historic='boundary_stone' or historic='milestone' or historic='rune_stone' or historic='highwater_mark' or amenity='fountain' or boundary='marker'";

        createFilteredShape(fs_sie03_p, atkisFilter, "Sonstiges", path+"temp/AX_Sonstiges_P.shp", true); // 9309
        createFilteredShape(fs_osm_p, osmFilter, "SonstigesP", path+"temp/SonstigesP_P.shp", false); // 1942
        createFilteredShape(fs_osm_l, osmFilter, "SonstigesP", path+"temp/SonstigesP_L.shp", false); // 0
        createFilteredShape(fs_osm_f, osmFilter, "SonstigesP", path+"temp/SonstigesP_F.shp", false); // 34

        SimpleFeatureSource fs_atkis_sonstiges_p = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Sonstiges_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_sonstigesP_p = FileDataStoreFinder.getDataStore(new File(path+"temp/SonstigesP_P.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_sonstigesP_l = FileDataStoreFinder.getDataStore(new File(path+"temp/SonstigesP_L.shp")).getFeatureSource();
        SimpleFeatureSource fs_osm_sonstigesP_f = FileDataStoreFinder.getDataStore(new File(path+"temp/SonstigesP_F.shp")).getFeatureSource();

        System.out.print("Anzahl Sonstiges-Objekt ATKIS: " + fs_atkis_sonstiges_p.getFeatures().size() + "\n");
        System.out.print("Anzahl Sonstiges-Objekt OSM: " + fs_osm_sonstigesP_p.getFeatures().size() + "\n");
        System.out.print("Anzahl Sonstiges-Objekt OSM: " + fs_osm_sonstigesP_l.getFeatures().size() + "\n");
        System.out.print("Anzahl Sonstiges-Objekt OSM: " + fs_osm_sonstigesP_f.getFeatures().size() + "\n");
        writeObjectlist(fs_osm_sonstigesP_f, path+"osm_sonstigesP_f.txt", new String[]{"historic", "amenity", "boundary"});
        
        convertAreaToPoint(fs_osm_sonstigesP_f, fs_osm_sonstigesP_p); // 34
        
        //Punkte aus dem gleichen Datensatz in Buffer löschen
        removeDuplicatePoints(fs_atkis_sonstiges_p, 0.1); // 4
        removeDuplicatePoints(fs_atkis_sonstiges_p, 1.0); // 
        removeDuplicatePoints(fs_atkis_sonstiges_p, 3.0); // 
        removeDuplicatePoints(fs_osm_sonstigesP_p, 0.1); // 
        removeDuplicatePoints(fs_osm_sonstigesP_p, 1.0); // 
        removeDuplicatePoints(fs_osm_sonstigesP_p, 3.0); // 
        
        System.out.print("Anzahl Sonstiges-Objekt ATKIS: " + fs_atkis_sonstiges_p.getFeatures().size() + "\n");// 
        System.out.print("Anzahl Sonstiges-Objekt OSM: " + fs_osm_sonstigesP_p.getFeatures().size() + "\n");	// 
        
        positionPointToPoint(fs_atkis_sonstiges_p, fs_osm_sonstigesP_p, 1.0); // 42
        positionPointToPoint(fs_atkis_sonstiges_p, fs_osm_sonstigesP_p, 2.0); // 64
        positionPointToPoint(fs_atkis_sonstiges_p, fs_osm_sonstigesP_p, 5.0); // 115
        //############################################################
        //## Ende AX_SonstigesBauwerkOderSonstigeEinrichtung Punkte ##
        //############################################################
        
        
      //############################################
      //## AX_EinrichtungInOeffentlichenBereichen ##
      //############################################
      atkisFilter = "OBJART_TXT='AX_EinrichtungInOeffentlichenBereichen'";
      osmFilter = "amenity NOT like '' and amenity NOT like 'fountain'";

      createFilteredShape(fs_sie03_p, atkisFilter, "AX_Oeffentlich", path+"temp/AX_Oeffentlich_P.shp", true); // 2383
      createFilteredShape(fs_osm_p, osmFilter, "Oeffentlich", path+"temp/Oeffentlich_P.shp", false); // 35474
      createFilteredShape(fs_osm_l, osmFilter, "Oeffentlich", path+"temp/Oeffentlich_L.shp", false); // 78
      createFilteredShape(fs_osm_f, osmFilter, "Oeffentlich", path+"temp/Oeffentlich_F.shp", false); // 7232

      SimpleFeatureSource fs_atkis_oeffentlich_p = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Oeffentlich_P.shp")).getFeatureSource();
      SimpleFeatureSource fs_osm_oeffentlich_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Oeffentlich_P.shp")).getFeatureSource();
      SimpleFeatureSource fs_osm_oeffentlich_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Oeffentlich_L.shp")).getFeatureSource();
      SimpleFeatureSource fs_osm_oeffentlich_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Oeffentlich_F.shp")).getFeatureSource();

      convertAreaToPoint(fs_osm_oeffentlich_l, fs_osm_oeffentlich_p); // 2
      convertAreaToPoint(fs_osm_oeffentlich_f, fs_osm_oeffentlich_p); // 7232
      
      //Punkte aus dem gleichen Datensatz in Buffer löschen
      removeDuplicatePoints(fs_atkis_oeffentlich_p, 5.0); // 0
      removeDuplicatePoints(fs_osm_oeffentlich_p, 0.1); // 50
      removeDuplicatePoints(fs_osm_oeffentlich_p, 0.2); // 10
      removeDuplicatePoints(fs_osm_oeffentlich_p, 0.5); // 32
      removeDuplicatePoints(fs_osm_oeffentlich_p, 1.0); // 174
      removeDuplicatePoints(fs_osm_oeffentlich_p, 2.0); // 1068
      removeDuplicatePoints(fs_osm_oeffentlich_p, 3.0); // 1708
      removeDuplicatePoints(fs_osm_oeffentlich_p, 4.0); // 1086
      removeDuplicatePoints(fs_osm_oeffentlich_p, 5.0); // 992
      
      System.out.print("Anzahl Oeffentlich-Objekt ATKIS: " + fs_atkis_oeffentlich_p.getFeatures().size() + "\n"); // 2383
      System.out.print("Anzahl Oeffentlich-Objekt OSM: " + fs_osm_oeffentlich_p.getFeatures().size() + "\n");	// 38157
      
      positionPointToPoint(fs_atkis_oeffentlich_p, fs_osm_oeffentlich_p, 1.0); // 0 sowohl Punkte als auch Flächenpunkte
      positionPointToPoint(fs_atkis_oeffentlich_p, fs_osm_oeffentlich_p, 2.0); // 0
      positionPointToPoint(fs_atkis_oeffentlich_p, fs_osm_oeffentlich_p, 5.0); // 1
      positionPointToPoint(fs_atkis_oeffentlich_p, fs_osm_oeffentlich_p,10.0); // 3
      positionPointToPoint(fs_atkis_oeffentlich_p, fs_osm_oeffentlich_p,20.0); // 11
      //#################################################
      //## Ende AX_EinrichtungInOeffentlichenBereichen ##
      //#################################################
        
	
    //#######################
    //## sie02_f / landuse ##
    //#######################
    //Intrinsische Überdeckungsprüfung ATKIS
    System.out.println("### Intrinsische Überdeckungsprüfung ATKIS ###");
    double totalarea_sie02_f = calculateArea(fs_sie02_f);
    Geometry union_sie02_f = bufferedUnionGeometry(fs_sie02_f, 0.0);
    double deltaarea_sie02_f = totalarea_sie02_f - union_sie02_f.getArea();
    System.out.print("Gesamtsumme Einzelflächen: " + df.format(totalarea_sie02_f) + "\n");
    System.out.print("Fläche Gesamtgeometrie: " + df.format(union_sie02_f.getArea()) + "\n");
	System.out.print("Flächenunterschied intrinsisch in sie02_f: " + df.format(deltaarea_sie02_f) + "\n");
	System.out.println("Anzahl Polygone: " + fs_sie02_f.getFeatures().size());
	
	//Intrinsische Überdeckungsprüfung OSM
	System.out.println("### Intrinsische Überdeckungsprüfung OSM ###");
	double totalarea_osm_landuse_f = calculateArea(fs_osm_landuse_f);
    Geometry union_osm_landuse_f = bufferedUnionGeometry(fs_osm_landuse_f, 0.0);
    double deltaarea_osm_landuse_f = totalarea_osm_landuse_f - union_osm_landuse_f.getArea();
    System.out.print("Gesamtsumme Einzelflächen: " + df.format(totalarea_osm_landuse_f) + "\n");
    System.out.print("Fläche Gesamtgeometrie: " + df.format(union_osm_landuse_f.getArea()) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM Landuse: " + df.format(deltaarea_osm_landuse_f) + "\n");
	System.out.println("Anzahl Polygone: " + fs_osm_landuse_f.getFeatures().size());
	System.out.println("######");
    //############################
    //## Ende sie02_f / landuse ##
    //############################
	
	//##############
	//## AX_Halde ##
	//##############
	atkisFilter = "OBJART_TXT='AX_Halde'";
	osmFilter = "landuse='landfill'";
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_Halde", path+"temp/AX_Halde_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "Halde", path+"temp/Halde_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "Halde", path+"temp/Halde_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "Halde", path+"temp/Halde_F.shp", false);
	
	SimpleFeatureSource fs_atkis_halde_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Halde_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_halde_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Halde_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_halde_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Halde_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_halde_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Halde_F.shp")).getFeatureSource();
	
	double totalarea_atkis_halde = calculateArea(fs_atkis_halde_f);
	double totalarea_osm_halde = calculateArea(fs_osm_halde_f);
	Geometry union_atkis_halde = bufferedUnionGeometry(fs_atkis_halde_f, 0.0);
	Geometry union_osm_halde = bufferedUnionGeometry(fs_osm_halde_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_halde = totalarea_atkis_halde - union_atkis_halde.getArea();
	double deltaarea_osm_halde = totalarea_osm_halde - union_osm_halde.getArea();
	System.out.print("Gesamtsumme Einzelflächen: " + df.format(totalarea_atkis_halde) + "\n");
	System.out.print("Fläche Gesamtgeometrie: " + df.format(union_atkis_halde.getArea()) + "\n");
	System.out.print("Flächenunterschied intrinsisch in ATKIS-Halde: " + df.format(deltaarea_atkis_halde) + "\n");
	System.out.print("Gesamtsumme Einzelflächen: " + df.format(totalarea_osm_halde) + "\n");
	System.out.print("Fläche Gesamtgeometrie: " + df.format(union_osm_halde.getArea()) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-Halde: " + df.format(deltaarea_osm_halde) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-Halde: " + df.format(union_atkis_halde.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-Halde: " + df.format(union_osm_halde.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche Halde: " + df.format(union_atkis_halde.intersection(union_osm_halde).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Halde (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_halde_f, 5.0).intersection(union_osm_halde).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Halde (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_halde_f, 10.0).intersection(union_osm_halde).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Halde (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_halde_f, 15.0).intersection(union_osm_halde).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'landfill'";
	Geometry union_osm_nicht_halde = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-Halde mit OSM-Nicht-Halde: " + df.format(union_atkis_halde.intersection(union_osm_nicht_halde).getArea()) + "\n");
	
//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_Halde\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_halde_f.getFeatures().size()+"\t"+fs_osm_halde_f.getFeatures().size()+"\t"+fs_osm_halde_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_halde.getArea())+"\t"+df.format(union_osm_halde.getArea())+"\t"+fs_osm_halde_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_halde)+"\t"+df.format(deltaarea_osm_halde)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_halde.intersection(union_osm_halde).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_halde_f, 5.0).intersection(union_osm_halde).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_halde_f, 10.0).intersection(union_osm_halde).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_halde_f, 15.0).intersection(union_osm_halde).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_Halde mit OSM-Nicht-Halde-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_halde.intersection(union_osm_nicht_halde).getArea())+"\tkm²\t\n");
	//###################
	//## Ende AX_Halde ##
	//###################
	
	
	//#################
	//## AX_Friedhof ##
	//#################
	atkisFilter = "OBJART_TXT='AX_Friedhof'";
	osmFilter = "landuse='cemetery'";
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_Friedhof", path+"temp/AX_Friedhof_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "Friedhof", path+"temp/Friedhof_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "Friedhof", path+"temp/Friedhof_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "Friedhof", path+"temp/Friedhof_F.shp", false);
	
	SimpleFeatureSource fs_atkis_friedhof_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Friedhof_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_friedhof_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Friedhof_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_friedhof_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Friedhof_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_friedhof_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Friedhof_F.shp")).getFeatureSource();
	
	double totalarea_atkis_friedhof = calculateArea(fs_atkis_friedhof_f);
	double totalarea_osm_friedhof = calculateArea(fs_osm_friedhof_f);
	Geometry union_atkis_friedhof = bufferedUnionGeometry(fs_atkis_friedhof_f, 0.0);
	Geometry union_osm_friedhof = bufferedUnionGeometry(fs_osm_friedhof_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_friedhof = totalarea_atkis_friedhof - union_atkis_friedhof.getArea();
	double deltaarea_osm_friedhof = totalarea_osm_friedhof - union_osm_friedhof.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-Friedhof: " + df.format(deltaarea_atkis_friedhof) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-Friedhof: " + df.format(deltaarea_osm_friedhof) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-Friedhof: " + df.format(union_atkis_friedhof.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-Friedhof: " + df.format(union_osm_friedhof.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche Friedhof: " + df.format(union_atkis_friedhof.intersection(union_osm_friedhof).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Friedhof (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_friedhof_f, 5.0).intersection(union_osm_friedhof).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Friedhof (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_friedhof_f, 10.0).intersection(union_osm_friedhof).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Friedhof (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_friedhof_f, 15.0).intersection(union_osm_friedhof).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'cemetery'";
	Geometry union_osm_nicht_friedhof = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-Friedhof mit OSM-Nicht-Friedhof: " + df.format(union_atkis_friedhof.intersection(union_osm_nicht_friedhof).getArea()) + "\n");

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_Friedhof\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_friedhof_f.getFeatures().size()+"\t"+fs_osm_friedhof_f.getFeatures().size()+"\t"+fs_osm_friedhof_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_friedhof.getArea())+"\t"+df.format(union_osm_friedhof.getArea())+"\t"+fs_osm_friedhof_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_friedhof)+"\t"+df.format(deltaarea_osm_friedhof)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_friedhof.intersection(union_osm_friedhof).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_friedhof_f, 5.0).intersection(union_osm_friedhof).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_friedhof_f, 10.0).intersection(union_osm_friedhof).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_friedhof_f, 15.0).intersection(union_osm_friedhof).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_Friedhof mit OSM-Nicht-Friedhof-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_friedhof.intersection(union_osm_nicht_friedhof).getArea())+"\tkm²\t\n");
	//######################
	//## Ende AX_Friedhof ##
	//######################
	
	
	//#######################
	//## AX_Wohnbauflaeche ##
	//#######################
	atkisFilter = "OBJART_TXT='AX_Wohnbauflaeche'";
	osmFilter = "landuse='residential'";
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_Wohnbauflaeche", path+"temp/AX_Wohnbauflaeche_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "Wohnbauflaeche", path+"temp/Wohnbauflaeche_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "Wohnbauflaeche", path+"temp/Wohnbauflaeche_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "Wohnbauflaeche", path+"temp/Wohnbauflaeche_F.shp", false);
	
	SimpleFeatureSource fs_atkis_wohnbauflaeche_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Wohnbauflaeche_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_wohnbauflaeche_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Wohnbauflaeche_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_wohnbauflaeche_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Wohnbauflaeche_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_wohnbauflaeche_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Wohnbauflaeche_F.shp")).getFeatureSource();
	
	double totalarea_atkis_wohnbauflaeche = calculateArea(fs_atkis_wohnbauflaeche_f);
	double totalarea_osm_wohnbauflaeche = calculateArea(fs_osm_wohnbauflaeche_f);
	Geometry union_atkis_wohnbauflaeche = bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 0.0);
	Geometry union_osm_wohnbauflaeche = bufferedUnionGeometry(fs_osm_wohnbauflaeche_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_wohnbauflaeche = totalarea_atkis_wohnbauflaeche - union_atkis_wohnbauflaeche.getArea();
	double deltaarea_osm_wohnbauflaeche = totalarea_osm_wohnbauflaeche - union_osm_wohnbauflaeche.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-Wohnbauflaeche: " + df.format(deltaarea_atkis_wohnbauflaeche) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-Wohnbauflaeche: " + df.format(deltaarea_osm_wohnbauflaeche) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-Wohnbauflaeche: " + df.format(union_atkis_wohnbauflaeche.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-Wohnbauflaeche: " + df.format(union_osm_wohnbauflaeche.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche Wohnbauflaeche: " + df.format(union_atkis_wohnbauflaeche.intersection(union_osm_wohnbauflaeche).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Wohnbauflaeche (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 5.0).intersection(union_osm_wohnbauflaeche).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Wohnbauflaeche (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 10.0).intersection(union_osm_wohnbauflaeche).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Wohnbauflaeche (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 15.0).intersection(union_osm_wohnbauflaeche).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'residential'";
	Geometry union_osm_nicht_wohnbauflaeche = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-Wohnbauflaeche mit OSM-Nicht-Wohnbauflaeche: " + df.format(union_atkis_wohnbauflaeche.intersection(union_osm_nicht_wohnbauflaeche).getArea()) + "\n");

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_Wohnbauflaeche\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_wohnbauflaeche_f.getFeatures().size()+"\t"+fs_osm_wohnbauflaeche_f.getFeatures().size()+"\t"+fs_osm_wohnbauflaeche_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_wohnbauflaeche.getArea())+"\t"+df.format(union_osm_wohnbauflaeche.getArea())+"\t"+fs_osm_wohnbauflaeche_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_wohnbauflaeche)+"\t"+df.format(deltaarea_osm_wohnbauflaeche)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_wohnbauflaeche.intersection(union_osm_wohnbauflaeche).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 5.0).intersection(union_osm_wohnbauflaeche).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 10.0).intersection(union_osm_wohnbauflaeche).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_wohnbauflaeche_f, 15.0).intersection(union_osm_wohnbauflaeche).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_Wohnbauflaeche mit OSM-Nicht-Wohnbauflaeche-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_wohnbauflaeche.intersection(union_osm_nicht_wohnbauflaeche).getArea())+"\tkm²\t\n");
	//############################
	//## Ende AX_Wohnbauflaeche ##
	//############################
	
	
	//#########################################
	//## AX_SportFreizeitUndErholungsflaeche ##
	//#########################################
	atkisFilter = "OBJART_TXT='AX_SportFreizeitUndErholungsflaeche'";
	osmFilter = "landuse='recreation_ground' or landuse='grass' or landuse='allotments'";
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_SportFreizeitUndErholungsflaeche", path+"temp/AX_SportFreizeitUndErholungsflaeche_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "SportFreizeitUndErholungsflaeche", path+"temp/SportFreizeitUndErholungsflaeche_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "SportFreizeitUndErholungsflaeche", path+"temp/SportFreizeitUndErholungsflaeche_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "SportFreizeitUndErholungsflaeche", path+"temp/SportFreizeitUndErholungsflaeche_F.shp", false);
	
	SimpleFeatureSource fs_atkis_sportfreizeit_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_SportFreizeitUndErholungsflaeche_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_sportfreizeit_p = FileDataStoreFinder.getDataStore(new File(path+"temp/SportFreizeitUndErholungsflaeche_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_sportfreizeit_l = FileDataStoreFinder.getDataStore(new File(path+"temp/SportFreizeitUndErholungsflaeche_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_sportfreizeit_f = FileDataStoreFinder.getDataStore(new File(path+"temp/SportFreizeitUndErholungsflaeche_F.shp")).getFeatureSource();
	
	double totalarea_atkis_sportfreizeit = calculateArea(fs_atkis_sportfreizeit_f);
	double totalarea_osm_sportfreizeit = calculateArea(fs_osm_sportfreizeit_f);
	Geometry union_atkis_sportfreizeit = bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 0.0);
	Geometry union_osm_sportfreizeit = bufferedUnionGeometry(fs_osm_sportfreizeit_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_sportfreizeit = totalarea_atkis_sportfreizeit - union_atkis_sportfreizeit.getArea();
	double deltaarea_osm_sportfreizeit = totalarea_osm_sportfreizeit - union_osm_sportfreizeit.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-SportFreizeitUndErholungsflaeche: " + df.format(deltaarea_atkis_sportfreizeit) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-SportFreizeitUndErholungsflaeche: " + df.format(deltaarea_osm_sportfreizeit) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-SportFreizeitUndErholungsflaeche: " + df.format(union_atkis_sportfreizeit.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-SportFreizeitUndErholungsflaeche: " + df.format(union_osm_sportfreizeit.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche SportFreizeitUndErholungsflaeche: " + df.format(union_atkis_sportfreizeit.intersection(union_osm_sportfreizeit).getArea()) + "\n");
	System.out.print("Überschneidungsfläche SportFreizeitUndErholungsflaeche (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 5.0).intersection(union_osm_sportfreizeit).getArea()) + "\n");
	System.out.print("Überschneidungsfläche SportFreizeitUndErholungsflaeche (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 10.0).intersection(union_osm_sportfreizeit).getArea()) + "\n");
	System.out.print("Überschneidungsfläche SportFreizeitUndErholungsflaeche (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 15.0).intersection(union_osm_sportfreizeit).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'recreation_ground' and landuse NOT LIKE 'grass' and landuse NOT LIKE 'allotments'";
	Geometry union_osm_nicht_sportfreizeit = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-SportFreizeitUndErholungsflaeche mit OSM-Nicht-SportFreizeitUndErholungsflaeche: " + df.format(union_atkis_sportfreizeit.intersection(union_osm_nicht_sportfreizeit).getArea()) + "\n");

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_SportFreizeitUndErholungsflaeche\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_sportfreizeit_f.getFeatures().size()+"\t"+fs_osm_sportfreizeit_f.getFeatures().size()+"\t"+fs_osm_sportfreizeit_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_sportfreizeit.getArea())+"\t"+df.format(union_osm_sportfreizeit.getArea())+"\t"+fs_osm_sportfreizeit_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_sportfreizeit)+"\t"+df.format(deltaarea_osm_sportfreizeit)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_sportfreizeit.intersection(union_osm_sportfreizeit).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 5.0).intersection(union_osm_sportfreizeit).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 10.0).intersection(union_osm_sportfreizeit).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_sportfreizeit_f, 15.0).intersection(union_osm_sportfreizeit).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_SportFreizeitUndErholungsflaeche mit OSM-Nicht-SportFreizeitUndErholungsflaeche-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_sportfreizeit.intersection(union_osm_nicht_sportfreizeit).getArea())+"\tkm²\t\n");
	//##############################################
	//## Ende AX_SportFreizeitUndErholungsflaeche ##
	//##############################################
	
	
	//###################################
	//## AX_IndustrieUndGewerbeflaeche ##
	//###################################
	atkisFilter = "OBJART_TXT='AX_IndustrieUndGewerbeflaeche'";
	osmFilter = "landuse='industrial' or landuse='commercial' or landuse='retail'";
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_IndustrieUndGewerbeflaeche", path+"temp/AX_IndustrieUndGewerbeflaeche_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "IndustrieUndGewerbeflaeche", path+"temp/IndustrieUndGewerbeflaeche_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "IndustrieUndGewerbeflaeche", path+"temp/IndustrieUndGewerbeflaeche_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "IndustrieUndGewerbeflaeche", path+"temp/IndustrieUndGewerbeflaeche_F.shp", false);
	
	SimpleFeatureSource fs_atkis_industrie_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_IndustrieUndGewerbeflaeche_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_industrie_p = FileDataStoreFinder.getDataStore(new File(path+"temp/IndustrieUndGewerbeflaeche_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_industrie_l = FileDataStoreFinder.getDataStore(new File(path+"temp/IndustrieUndGewerbeflaeche_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_industrie_f = FileDataStoreFinder.getDataStore(new File(path+"temp/IndustrieUndGewerbeflaeche_F.shp")).getFeatureSource();
	
	double totalarea_atkis_industrie = calculateArea(fs_atkis_industrie_f);
	double totalarea_osm_industrie = calculateArea(fs_osm_industrie_f);
	Geometry union_atkis_industrie = bufferedUnionGeometry(fs_atkis_industrie_f, 0.0);
	Geometry union_osm_industrie = bufferedUnionGeometry(fs_osm_industrie_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_industrie = totalarea_atkis_industrie - union_atkis_industrie.getArea();
	double deltaarea_osm_industrie = totalarea_osm_industrie - union_osm_industrie.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-IndustrieUndGewerbeflaeche: " + df.format(deltaarea_atkis_industrie) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-IndustrieUndGewerbeflaeche: " + df.format(deltaarea_osm_industrie) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-IndustrieUndGewerbeflaeche: " + df.format(union_atkis_industrie.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-IndustrieUndGewerbeflaeche: " + df.format(union_osm_industrie.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche IndustrieUndGewerbeflaeche: " + df.format(union_atkis_industrie.intersection(union_osm_industrie).getArea()) + "\n");
	System.out.print("Überschneidungsfläche IndustrieUndGewerbeflaeche (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_industrie_f, 5.0).intersection(union_osm_industrie).getArea()) + "\n");
	System.out.print("Überschneidungsfläche IndustrieUndGewerbeflaeche (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_industrie_f, 10.0).intersection(union_osm_industrie).getArea()) + "\n");
	System.out.print("Überschneidungsfläche IndustrieUndGewerbeflaeche (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_industrie_f, 15.0).intersection(union_osm_industrie).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'industrial' and landuse NOT LIKE 'commercial' and landuse NOT LIKE 'retail'";
	Geometry union_osm_nicht_industrie = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-IndustrieUndGewerbeflaeche mit OSM-Nicht-IndustrieUndGewerbeflaeche: " + df.format(union_atkis_industrie.intersection(union_osm_nicht_industrie).getArea()) + "\n");

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_IndustrieUndGewerbeflaeche\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_industrie_f.getFeatures().size()+"\t"+fs_osm_industrie_f.getFeatures().size()+"\t"+fs_osm_industrie_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_industrie.getArea())+"\t"+df.format(union_osm_industrie.getArea())+"\t"+fs_osm_industrie_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_industrie)+"\t"+df.format(deltaarea_osm_industrie)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_industrie.intersection(union_osm_industrie).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_industrie_f, 5.0).intersection(union_osm_industrie).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_industrie_f, 10.0).intersection(union_osm_industrie).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_industrie_f, 15.0).intersection(union_osm_industrie).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_IndustrieUndGewerbeflaeche mit OSM-Nicht-IndustrieUndGewerbeflaeche-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_industrie.intersection(union_osm_nicht_industrie).getArea())+"\tkm²\t\n");
	//########################################
	//## Ende AX_IndustrieUndGewerbeflaeche ##
	//########################################
	
	
	//###############################
	//## AX_TagebauGrubeSteinbruch ##
	//###############################
	atkisFilter = "OBJART_TXT='AX_TagebauGrubeSteinbruch'";
	osmFilter = "landuse='quarry'";
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_TagebauGrubeSteinbruch", path+"temp/AX_TagebauGrubeSteinbruch_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "TagebauGrubeSteinbruch", path+"temp/TagebauGrubeSteinbruch_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "TagebauGrubeSteinbruch", path+"temp/TagebauGrubeSteinbruch_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "TagebauGrubeSteinbruch", path+"temp/TagebauGrubeSteinbruch_F.shp", false);
	
	SimpleFeatureSource fs_atkis_tagebau_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_TagebauGrubeSteinbruch_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_tagebau_p = FileDataStoreFinder.getDataStore(new File(path+"temp/TagebauGrubeSteinbruch_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_tagebau_l = FileDataStoreFinder.getDataStore(new File(path+"temp/TagebauGrubeSteinbruch_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_tagebau_f = FileDataStoreFinder.getDataStore(new File(path+"temp/TagebauGrubeSteinbruch_F.shp")).getFeatureSource();
	
	double totalarea_atkis_tagebau = calculateArea(fs_atkis_tagebau_f);
	double totalarea_osm_tagebau = calculateArea(fs_osm_tagebau_f);
	Geometry union_atkis_tagebau = bufferedUnionGeometry(fs_atkis_tagebau_f, 0.0);
	Geometry union_osm_tagebau = bufferedUnionGeometry(fs_osm_tagebau_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_tagebau = totalarea_atkis_tagebau - union_atkis_tagebau.getArea();
	double deltaarea_osm_tagebau = totalarea_osm_tagebau - union_osm_tagebau.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-TagebauGrubeSteinbruch: " + df.format(deltaarea_atkis_tagebau) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-TagebauGrubeSteinbruch: " + df.format(deltaarea_osm_tagebau) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-TagebauGrubeSteinbruch: " + df.format(union_atkis_tagebau.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-TagebauGrubeSteinbruch: " + df.format(union_osm_tagebau.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche TagebauGrubeSteinbruch: " + df.format(union_atkis_tagebau.intersection(union_osm_tagebau).getArea()) + "\n");
	System.out.print("Überschneidungsfläche TagebauGrubeSteinbruch (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_tagebau_f, 5.0).intersection(union_osm_tagebau).getArea()) + "\n");
	System.out.print("Überschneidungsfläche TagebauGrubeSteinbruch (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_tagebau_f, 10.0).intersection(union_osm_tagebau).getArea()) + "\n");
	System.out.print("Überschneidungsfläche TagebauGrubeSteinbruch (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_tagebau_f, 15.0).intersection(union_osm_tagebau).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'quarry'";
	Geometry union_osm_nicht_tagebau = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-TagebauGrubeSteinbruch mit OSM-Nicht-TagebauGrubeSteinbruch: " + df.format(union_atkis_tagebau.intersection(union_osm_nicht_tagebau).getArea()) + "\n");

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_TagebauGrubeSteinbruch\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_tagebau_f.getFeatures().size()+"\t"+fs_osm_tagebau_f.getFeatures().size()+"\t"+fs_osm_tagebau_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_tagebau.getArea())+"\t"+df.format(union_osm_tagebau.getArea())+"\t"+fs_osm_tagebau_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_tagebau)+"\t"+df.format(deltaarea_osm_tagebau)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_tagebau.intersection(union_osm_tagebau).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_tagebau_f, 5.0).intersection(union_osm_tagebau).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_tagebau_f, 10.0).intersection(union_osm_tagebau).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_tagebau_f, 15.0).intersection(union_osm_tagebau).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_TagebauGrubeSteinbruch mit OSM-Nicht-TagebauGrubeSteinbruch-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_tagebau.intersection(union_osm_nicht_tagebau).getArea())+"\tkm²\t\n");
	//####################################
	//## Ende AX_TagebauGrubeSteinbruch ##
	//####################################
	
	
	//#######################		// sehr schwierig einzuordnen, da kein eindeutiger OSM-Tag besteht (Untertage)
	//## AX_Bergbaubetrieb ##		// Thematische Genauigkeit als möglicher Indikator für falsche Zuordnung 
	//#######################		// https://wiki.openstreetmap.org/wiki/DE:Tag:landuse%3Dquarry   https://wiki.openstreetmap.org/wiki/Tag:industrial%3Dmine
	atkisFilter = "OBJART_TXT='AX_Bergbaubetrieb'";
	osmFilter = "landuse='industrial'"; //man_made='mineshaft'->keine Überschneidung ||| landuse='mine' nicht vorhanden (keine Fehler) ||| landuse='quarry'->Überschneidung nur 55145,0731
	
	createFilteredShape(fs_sie02_f, atkisFilter, "AX_Bergbaubetrieb", path+"temp/AX_Bergbaubetrieb_F.shp", true);
	createFilteredShape(fs_osm_landuse_p, osmFilter, "Bergbaubetrieb", path+"temp/Bergbaubetrieb_P.shp", false);
	createFilteredShape(fs_osm_landuse_l, osmFilter, "Bergbaubetrieb", path+"temp/Bergbaubetrieb_L.shp", false);
	createFilteredShape(fs_osm_landuse_f, osmFilter, "Bergbaubetrieb", path+"temp/Bergbaubetrieb_F.shp", false);
	
	SimpleFeatureSource fs_atkis_bergbaubetrieb_f = FileDataStoreFinder.getDataStore(new File(path+"temp/AX_Bergbaubetrieb_F.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_bergbaubetrieb_p = FileDataStoreFinder.getDataStore(new File(path+"temp/Bergbaubetrieb_P.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_bergbaubetrieb_l = FileDataStoreFinder.getDataStore(new File(path+"temp/Bergbaubetrieb_L.shp")).getFeatureSource();
	SimpleFeatureSource fs_osm_bergbaubetrieb_f = FileDataStoreFinder.getDataStore(new File(path+"temp/Bergbaubetrieb_F.shp")).getFeatureSource();
	
	double totalarea_atkis_bergbaubetrieb = calculateArea(fs_atkis_bergbaubetrieb_f);
	double totalarea_osm_bergbaubetrieb = calculateArea(fs_osm_bergbaubetrieb_f);
	Geometry union_atkis_bergbaubetrieb = bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 0.0);
	Geometry union_osm_bergbaubetrieb = bufferedUnionGeometry(fs_osm_bergbaubetrieb_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_bergbaubetrieb = totalarea_atkis_bergbaubetrieb - union_atkis_bergbaubetrieb.getArea();
	double deltaarea_osm_bergbaubetrieb = totalarea_osm_bergbaubetrieb - union_osm_bergbaubetrieb.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-Bergbaubetrieb: " + df.format(deltaarea_atkis_bergbaubetrieb) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-Bergbaubetrieb: " + df.format(deltaarea_osm_bergbaubetrieb) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-Bergbaubetrieb: " + df.format(union_atkis_bergbaubetrieb.getArea()) + "\n");
	System.out.print("Gesamtfläche OSM-Bergbaubetrieb: " + df.format(union_osm_bergbaubetrieb.getArea()) + "\n");
	
	System.out.print("Überschneidungsfläche Bergbaubetrieb: " + df.format(union_atkis_bergbaubetrieb.intersection(union_osm_bergbaubetrieb).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Bergbaubetrieb (5m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 5.0).intersection(union_osm_bergbaubetrieb).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Bergbaubetrieb (10m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 10.0).intersection(union_osm_bergbaubetrieb).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Bergbaubetrieb (15m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 15.0).intersection(union_osm_bergbaubetrieb).getArea()) + "\n");
	
	//Thematische Genauigkeit
	osmFilter = "landuse NOT LIKE 'industrial'";
	Geometry union_osm_nicht_bergbaubetrieb = bufferedUnionGeometry(fs_osm_landuse_f, 0.0, osmFilter);
	System.out.print("Überschneidungsfläche ATKIS-Bergbaubetrieb mit OSM-Nicht-Bergbaubetrieb: " + df.format(union_atkis_bergbaubetrieb.intersection(union_osm_nicht_bergbaubetrieb).getArea()) + "\n");

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("AX_Bergbaubetrieb\tATKIS\tOSM\tFalsche OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_bergbaubetrieb_f.getFeatures().size()+"\t"+fs_osm_bergbaubetrieb_f.getFeatures().size()+"\t"+fs_osm_bergbaubetrieb_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_bergbaubetrieb.getArea())+"\t"+df.format(union_osm_bergbaubetrieb.getArea())+"\t"+fs_osm_bergbaubetrieb_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_bergbaubetrieb)+"\t"+df.format(deltaarea_osm_bergbaubetrieb)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_bergbaubetrieb.intersection(union_osm_bergbaubetrieb).getArea())+"\tkm²\t\n");
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 5.0).intersection(union_osm_bergbaubetrieb).getArea())+"\tkm²\t\n");
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 10.0).intersection(union_osm_bergbaubetrieb).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_bergbaubetrieb_f, 15.0).intersection(union_osm_bergbaubetrieb).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
//	System.out.print("Verschneiden von AX_Bergbaubetrieb mit OSM-Nicht-Bergbaubetrieb-Daten\t\t\t\n");
//	System.out.print("\t"+df.format(union_atkis_bergbaubetrieb.intersection(union_osm_nicht_bergbaubetrieb).getArea())+"\tkm²\t\n");
	//############################
	//## Ende AX_Bergbaubetrieb ##
	//############################
	
	
	
	//#############
	//## Gebäude ## 
	//#############	
//	osmFilter = "building NOT LIKE ''";
//	
//	createFilteredShape(fs_osm_p, osmFilter, "Gebaeude", path+"temp/Gebaeude_P.shp", false);
//	createFilteredShape(fs_osm_l, osmFilter, "Gebaeude", path+"temp/Gebaeude_L.shp", false);
//	createFilteredShape(fs_osm_f, osmFilter, "Gebaeude", path+"temp/Gebaeude_F.shp", false);
	
	//Intrinsisch: Adressfelder
	SimpleFeatureCollection fc_adresse = fs_osm_gebaeude_f.getFeatures(CQL.toFilter("addr_stree NOT LIKE ''"));
	System.out.print("Gebäude mit Adressfeld: " + fc_adresse.size() + "\n");
    
	double totalarea_atkis_gebaeude = calculateArea(fs_atkis_gebaeude_f);
	double totalarea_osm_gebaeude = calculateArea(fs_osm_gebaeude_f);
	Geometry union_osm_gebaeude = bufferedUnionGeometry(fs_osm_gebaeude_f, 0.0);
	Geometry union_atkis_gebaeude = bufferedUnionGeometry(fs_atkis_gebaeude_f, 0.0);
	
	//Überdeckung im eigenen Datensatz: Differenz zwischen Summe Einzelflächengröße und Flächengröße von Union-Geometrie
	double deltaarea_atkis_gebaeude = totalarea_atkis_gebaeude - union_atkis_gebaeude.getArea();
	double deltaarea_osm_gebaeude = totalarea_osm_gebaeude - union_osm_gebaeude.getArea();
	System.out.print("Flächenunterschied intrinsisch in ATKIS-Gebaeude: " + df.format(deltaarea_atkis_gebaeude) + "\n");
	System.out.print("Flächenunterschied intrinsisch in OSM-Gebaeude: " + df.format(deltaarea_osm_gebaeude) + "\n");

	//Flächenvergleiche
	System.out.print("Gesamtfläche ATKIS-Gebaeude: " + df.format(totalarea_atkis_gebaeude) + "\n");
	System.out.print("Gesamtfläche OSM-Gebaeude: " + df.format(totalarea_osm_gebaeude) + "\n");

	//** Gebufferte ATKIS-Daten mit OSM-Einzelflächen -- Nachteil: Flächenüberlagerungen werden nicht berücksichtigt bzw. gefiltert**	
	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	SimpleFeatureCollection indexedPolygonCollection = new SpatialIndexFeatureCollection(fs_osm_gebaeude_f.getFeatures());
	SimpleFeatureSource source = DataUtilities.source(indexedPolygonCollection);
	Filter filter = ff.intersects(ff.property("the_geom"), ff.literal(union_atkis_gebaeude));
	SimpleFeatureCollection results = source.getFeatures(filter);
	SimpleFeatureSource gebaeude = DataUtilities.source(results);
	System.out.println("Überschneidungsfläche Gebaeude: " + df.format(calculateArea(gebaeude)));
	// Zu hoher Rechenaufwand
	filter = ff.intersects(ff.property("the_geom"), ff.literal(bufferedUnionGeometry(fs_atkis_gebaeude_f, 1.0)));
	System.out.println("Überschneidungsfläche Gebaeude: (1m Buffer ATKIS): " + df.format(calculateArea(DataUtilities.source(source.getFeatures(filter)))));
	// *********************************************************************
	
	//** Gebufferte ATKIS-Daten mit OSM-Einzelflächen (ohne Spatial Index) -- mit Filterung doppelter Flächen **
	System.out.print("Überschneidungsfläche Gebaeude: " + df.format(union_atkis_gebaeude.intersection(union_osm_gebaeude).getArea()) + "\n");
	System.out.print("Überschneidungsfläche Gebaeude (1m Buffer ATKIS): " + df.format(bufferedUnionGeometry(fs_atkis_gebaeude_f, 1.0).intersection(union_osm_gebaeude).getArea()) + "\n");
	//*********************************************************************

//	//Ausgabe für Excel
//	System.out.print("Intrinsische Datensatzparameter\t\t\t\n");
//	System.out.print("Gebaeude\tATKIS\tOSM\tNicht vergleichbare OSM-Geometrietypwahl\n");
//	System.out.print("Anzahl Polygone\t"+fs_atkis_gebaeude_f.getFeatures().size()+"\t"+fs_osm_gebaeude_f.getFeatures().size()+"\t"+fs_osm_gebaeude_p.getFeatures().size()+" Nodes\n");
//	System.out.print("Gesamtsumme\t"+df.format(union_atkis_gebaeude.getArea())+"\t"+df.format(union_osm_gebaeude.getArea())+"\t"+fs_osm_gebaeude_l.getFeatures().size()+" Ways\n");
//	System.out.print("Intrinsische Überlappung\t"+df.format(deltaarea_atkis_gebaeude)+"\t"+df.format(deltaarea_osm_gebaeude)+"\n");
//	System.out.print("Verschneiden von ATKIS und OSM\t\t\t\n");
//	System.out.print(" 0 m Buffer\t"+df.format(union_atkis_gebaeude.intersection(union_osm_gebaeude).getArea())+"\tkm²\t\n");
//	System.out.println( sdf.format(Calendar.getInstance().getTime()) );//***
//	System.out.print(" 5 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_gebaeude_f, 5.0).intersection(union_osm_gebaeude).getArea())+"\tkm²\t\n");
//	System.out.println( sdf.format(Calendar.getInstance().getTime()) );//***
//	System.out.print("10 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_gebaeude_f, 10.0).intersection(union_osm_gebaeude).getArea())+"\tkm²\t\n");
//	System.out.print("15 m Buffer\t"+df.format(bufferedUnionGeometry(fs_atkis_gebaeude_f, 15.0).intersection(union_osm_gebaeude).getArea())+"\tkm²\t\n");
//	System.out.print("\t\t\t\n");
	//##################
	//## Ende Gebäude ##
	//##################


    //##################################
    //## Attributlisten als Textdatei ##
    //##################################
    String[] attributeList = {"name", "building", "man_made", "historic"};
    writeObjectlist(fs_osm_turm_p, path+"tower_attributes_p.txt", attributeList);
    writeObjectlist(fs_osm_turm_l, path+"tower_attributes_l.txt", attributeList);
    writeObjectlist(fs_atkis_turm_p, path+"atkis_tower_p.txt", new String[]{"OBJID", "NAM", "BEGINN", "ENDE", "Objektart"});
    writeObjectlist(fs_atkis_turm_f, path+"atkis_tower_f.txt", new String[]{"OBJID", "NAM", "BEGINN", "ENDE", "Objektart"});
    //#######################################
    //## Ende Attributlisten als Textdatei ##
    //#######################################

    //#########
    //## Map ##
    //#########
	Style style = SLD.createSimpleStyle(fs_osm_turm_l.getSchema());
	Layer layer = new FeatureLayer(fs_osm_turm_l, style);
	Style style2 = SLD.createSimpleStyle(fs_osm_f.getSchema());
	Layer layer2 = new FeatureLayer(fs_osm_f, style2);
    MapContent map = new MapContent();
    map.setTitle("Siedlungsanalyse");
    map.addLayer(layer);
    map.addLayer(layer2);
    JMapFrame show = new JMapFrame(map);
    show.enableLayerTable( true );
    show.enableToolBar(true);
    show.enableStatusBar(true);
    show.setVisible(true);
    //##############
    //## Ende Map ##
    //##############
    }
}