package com.mndk.kvm2m.core.vmap.reader;

import com.mndk.kvm2m.core.util.file.DirectoryManager;
import com.mndk.kvm2m.core.util.file.ZipManager;
import com.mndk.kvm2m.core.util.math.Vector2DH;
import com.mndk.kvm2m.core.vmap.*;
import com.mndk.kvm2m.core.db.common.TableColumn;
import com.mndk.kvm2m.core.db.common.TableColumns;
import com.mndk.kvm2m.core.vmap.type.VMapElementDataType;
import com.mndk.kvm2m.core.vmap.type.VMapElementGeomType;
import com.mndk.shapefile.ShpDbfDataIterator;
import com.mndk.shapefile.ShpDbfRecord;
import com.mndk.shapefile.shp.ShapeVector;
import com.mndk.shapefile.shp.ShapefileRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Map;


public class ShpZipMapReader extends VMapReader {


	@Override
	protected Map.Entry<VMapPayload.Geometry, VMapPayload.Data> getResult() throws IOException {

		VMapPayload.Geometry geometryPayload = new VMapPayload.Geometry();
		VMapPayload.Data dataPayload = new VMapPayload.Data();
		
		Throwable throwable = null;

		String mapFilePath = mapFile.getAbsolutePath();
		File zipDestination = new File(mapFilePath.substring(0, mapFilePath.lastIndexOf(".zip")) + "/");

		try {

			if(zipDestination.exists()) {
				zipDestination.delete();
			}

			// Extract all files in map file
			zipDestination.mkdir();
			ZipManager.extractZipFile(mapFile, zipDestination, "cp949");

			File[] shapeFiles = zipDestination.listFiles((dir, name) -> name.endsWith(".shp"));
			assert shapeFiles != null;

			if(shapeFiles.length == 0) {
				System.err.println("Huh? I can't found any .shp file in this folder! >:(");
			}

			long count = 0;

			for(File shapeFile : shapeFiles) {
				String filePath = shapeFile.getAbsolutePath();
				filePath = filePath.substring(0, filePath.length() - 4);
				String fileName = new File(filePath).getName();
				VMapElementDataType type = VMapElementDataType.fromLayerName(fileName);
				TableColumns columns = type.getColumns();

				try(ShpDbfDataIterator iterator =
							new ShpDbfDataIterator(filePath, Charset.forName("cp949"))) {

					for (ShpDbfRecord record : iterator) {
						// System.out.println(record.dBase);

						VMapPayload.Geometry.Record<?> geometryRecord = fromShpRecord(record.shape);

						Object[] dataRow = new Object[columns.getLength()];
						for(int i = 0; i < columns.getLength(); ++i) {
							TableColumn column = columns.get(i);
							dataRow[i] = record.dBase.getDataByField(column.getName());
						}

						VMapPayload.Data.Record dataRecord = new VMapPayload.Data.Record(type, dataRow);

						geometryPayload.put(count, geometryRecord);
						dataPayload.put(count, dataRecord);
						++count;
					}
				}
			}
		} catch(Throwable t) {
			throwable = t;
		}

		DirectoryManager.deleteDirectory(zipDestination);
		
		if(throwable != null) {
			if(throwable instanceof IOException) {
				throw (IOException) throwable;
			}
			else {
				throw new RuntimeException(throwable);
			}
		}

		return new AbstractMap.SimpleEntry<>(geometryPayload, dataPayload);
		
	}



	protected VMapPayload.Geometry.Record<?> fromShpRecord(ShapefileRecord record) {
		if(record instanceof ShapefileRecord.Polygon) {
			return new VMapPayload.Geometry.Record<>(
					VMapElementGeomType.POLYGON, fromPolygon((ShapefileRecord.Polygon) record));
		}
		else if(record instanceof ShapefileRecord.PolyLine) {
			return new VMapPayload.Geometry.Record<>(
					VMapElementGeomType.LINESTRING, fromLine((ShapefileRecord.PolyLine) record));
		}
		else if(record instanceof ShapefileRecord.Point) {
			return new VMapPayload.Geometry.Record<>(
					VMapElementGeomType.POINT, fromPoint((ShapefileRecord.Point) record));
		}
		return null;
	}
	
	
	
	private Vector2DH[][] fromPolygon(ShapefileRecord.Polygon polygon) {
		ShapeVector[][] points = polygon.points;
		Vector2DH[][] vertexList = new Vector2DH[polygon.points.length][];
		
		for(int j = 0; j < points.length; ++j) {
			int size = points[j].length;
			vertexList[j] = new Vector2DH[size];
			
			for(int i = 0; i < size; ++i) {
				ShapeVector vector = points[j][i];
				vertexList[j][i] = this.targetProjToWorldProjCoord(vector.x, vector.y);
			}
		}

		return vertexList;
	}
	

	
	private Vector2DH[][] fromLine(ShapefileRecord.PolyLine polyline) {
		ShapeVector[][] points = polyline.points;
		Vector2DH[][] vertexList = new Vector2DH[polyline.points.length][];
		
		for(int j = 0; j < points.length; ++j) {
			int size = points[j].length;
			vertexList[j] = new Vector2DH[size];
			
			for(int i = 0; i < size; ++i) {
				ShapeVector vector = points[j][i];
				vertexList[j][i] = this.targetProjToWorldProjCoord(vector.x, vector.y);
			}
		}

		return vertexList;
	}
	
	
	
	private Vector2DH fromPoint(ShapefileRecord.Point point) {
		return this.targetProjToWorldProjCoord(point.vector.x, point.vector.y);
	}

}
