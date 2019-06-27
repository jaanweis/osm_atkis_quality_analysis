package GeoModA.GeoTools;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

public class FreieTestfaelle {
	/*
	Dies ist ein Codeschnipsel des Hauptprogramms, welches zur Überprüfung der
	Methodik zur Schwerpunktberechnung verwendet wurde
	*/
	public void Testfall() {
	GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	Coordinate coords[] = {
			new Coordinate(1, 1),
			new Coordinate(1, 5),
			new Coordinate(1, 6),
			new Coordinate(2, 5),
			new Coordinate(2, 6),
			new Coordinate(3, 5),
			new Coordinate(3, 6),
			new Coordinate(4, 5),
			new Coordinate(4, 6),
			new Coordinate(5, 5),
			new Coordinate(5, 6),
			new Coordinate(6, 6),
			new Coordinate(6, 5),
			new Coordinate(6, 1),
			new Coordinate(1, 1)
	};
	System.out.print(geometryFactory.createLineString(coords).getCentroid());
	//POINT (3.467711274473074 3.863010539344238)
	System.out.print(geometryFactory.createPolygon(geometryFactory.createLinearRing(coords)).getCentroid());
	//POINT (3.528985507246377 3.3115942028985508)
	}
}