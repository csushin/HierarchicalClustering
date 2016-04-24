package test.java.com.apporiented.algorithm.clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.gdal.gdal.Band;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

import main.java.com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.Cluster;
import main.java.com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

public class HierarchicalClusteringTotalDist {
	private String[] modelList = {
			"CCCma-CanESM2_CCCma-CanRCM4",
		    "CCCma-CanESM2_SMHI-RCA4",
		    "CNRM-CERFACS-CNRM-CM5_CLMcom-CCLM4-8-17",
		    "CNRM-CERFACS-CNRM-CM5_SMHI-RCA4",
		    "CSIRO-QCCCE-CSIRO-Mk3-6-0_SMHI-RCA4",
		    "ICHEC-EC-EARTH_CLMcom-CCLM4-8-17",
		    "ICHEC-EC-EARTH_DMI-HIRHAM5",
		    "ICHEC-EC-EARTH_KNMI-RACMO22T",
		    "ICHEC-EC-EARTH_SMHI-RCA4",
		    "IPSL-IPSL-CM5A-MR_SMHI-RCA4",
		    "MIROC-MIROC5_SMHI-RCA4_v1",
		    "MOHC-HadGEM2-ES_CLMcom-CCLM4-8-17",
		    "MOHC-HadGEM2-ES_SMHI-RCA4",
		    "MOHC-HadGEM2-ES_KNMI-RACMO22T_v1",
		    "MPI-M-MPI-ESM-LR_CLMcom-CCLM4-8-17",
		    "MPI-M-MPI-ESM-LR_SMHI-RCA4",
		    "NCC-NorESM1-M_SMHI-RCA4",
		    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4"
	};
	private String[] metricList = {"TimeMean", "TimeSkewness", "TimeKurtosis", "TimeEntropy", "TimeQuadraticScore", "TimeCV", "TimeStd", "TimeIQR"};
	
	public class GeotiffInfo{
		public int width;
		public int height;
		public String projRef;
		public double[] geoInfo;
		public GeotiffInfo(){}
	}
	
	public class ComputeHCThread implements Runnable{
		public int startHIndex;
		public int endHIndex;
		public HashMap<String, File> parsers;
		public double[] result;
		public int width;
		public GeotiffInfo geoinfo;
		
		public ComputeHCThread(int startHIndex, int endHIndex, int width, double[] result, HashMap<String, File> parsers, GeotiffInfo geoInfo){
			this.startHIndex = startHIndex;
			this.endHIndex = endHIndex;
			this.width = width;
			this.parsers = parsers;
			this.result = result;
			this.geoinfo = geoInfo;
		}
		
		@Override
		public void run() {
			HashMap<String, Band> bandParsers = new HashMap<String, Band>();
			for(String key : this.parsers.keySet()){
				gdal.AllRegister();
				Dataset hDataset = gdal.Open(parsers.get(key).getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
				if (hDataset == null)
				{
					// parse Tiff Image
					System.err.println("GDALOpen failed - " + gdal.GetLastErrorNo());
					System.err.println(gdal.GetLastErrorMsg());
				}
				Band hBand = hDataset.GetRasterBand(1);
				if(!bandParsers.containsKey(key)) bandParsers.put(key, hBand);
				this.geoinfo.width = hDataset.getRasterXSize();
				this.geoinfo.height = hDataset.getRasterYSize();
				this.geoinfo.projRef = hDataset.GetProjectionRef();
				this.geoinfo.geoInfo = hDataset.GetGeoTransform();
			}
			// TODO Auto-generated method stub
			HashMap<String, String> historicalPts = new HashMap<String, String>();
			HashMap<String, Integer> hashmap = new HashMap<String, Integer>();
			for(int w=0; w<this.width; w++){
				for(int h=this.startHIndex; h<this.endHIndex; h++){
					String[] names = new String[modelList.length];
					double[][] dist = new double[modelList.length][modelList.length];
					double[][] cmddist = new double[modelList.length][modelList.length];
					for(int m=0; m<modelList.length; m++){
						names[m] = "O"+m;
						for(int n=0; n<modelList.length; n++){
							 if(n==m){
								 dist[m][n] = 0;
							 }
							 else if(n<m){
								 dist[m][n] = dist[n][m];
							 }
							 else{
								double[] temp = new double[1];
								Band hBand = bandParsers.get(modelList[m]+"_"+modelList[n]+"_OverallSum");
								if(hBand == null)
									System.out.println(modelList[m]+"_"+modelList[n] + ", " + w + ", " + h);
								hBand.ReadRaster(w, h, 1, 1, gdalconst.GDT_Float64, temp);
								dist[m][n] = temp[0];
							 }
						}
					}
					String testDist = Arrays.deepToString(dist);
					String content = "";
					if(historicalPts.containsKey(testDist))
						content = historicalPts.get(testDist);
					else{
						ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
						Cluster cluster = alg.performClustering(dist, names, new AverageLinkageStrategy());
						for(int m=0; m<names.length-1; m++){
						  	for(int n=m+1; n<names.length; n++){
						  		if(dist[m][n] == 0){
						  			cmddist[m][n] = Double.NaN;
						  		}
						  		else{
						  			cmddist[m][n] = cluster.computeCMDDistance(names[m], names[n]);
						  		}
						  		content += cmddist[m][n] + "|";
						  	}
						}
						historicalPts.put(testDist, content);
					}
					int hashId = 0;
					if(hashmap.containsKey(content))
						hashId = hashmap.get(content);
					else{
						hashId = hashmap.keySet().size()+1;
						hashmap.put(content, hashId);
					}
					int index = (int) (h*this.width + w);
					this.result[index] = hashId;
//					synchronized (System.out) {
//					    System.out.println(w + ", " + h);
//					  }
//					System.out.println("W is: " + w + " H is: " + h);	
				}
			}
			System.out.println("Yi Er San Finished!");	
//			save(serilization) hashmap
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/HierarchicalClst/TotalDistance/hash-" + 
								this.startHIndex + "-" + this.endHIndex + ".ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(hashmap);
				oos.close();
				fos.close();
				System.out.println("Hash Map is Saved!");	
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public class LoadTiffsBand implements Runnable{
		private File file; 
		private HashMap<String, Band> parsers;
		private String key;
		public GeotiffInfo info;

		
		public LoadTiffsBand(String key, File file, HashMap<String, Band> parsers, GeotiffInfo geoInfo){
			this.file = file;
			this.key = key;
			this.parsers = parsers;
			this.info = geoInfo;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			gdal.AllRegister();
			Dataset hDataset= gdal.Open(file.getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
			this.info.width = hDataset.GetRasterXSize();
			this.info.height = hDataset.GetRasterYSize();
			this.info.geoInfo = hDataset.GetGeoTransform();
			this.info.projRef = hDataset.GetProjectionRef();
			Band hBand = hDataset.GetRasterBand(1);
			if (hBand == null)
			{
				// parse Tiff Image
				System.err.println("GDALOpen failed - " + gdal.GetLastErrorNo());
				System.err.println(gdal.GetLastErrorMsg());
			}
			else{
				this.parsers.put(this.key, hBand);
			}
			
		}
	}
	
	

	
	public static void main(String[] args){
		HashMap<String, Band> parsers = new HashMap<String, Band>();
		HierarchicalClusteringTotalDist self = new HierarchicalClusteringTotalDist();
		GeotiffInfo geoInfo = self.new GeotiffInfo();
	    HashMap<String, File> filePath = new HashMap<String, File>();
//	    for(int i=0; i<self.modelList.length; i++){
	    	String baseDir = "/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/OverallSum/";
//	    	String key = self.modelList[i];
	    	filePath = self.getAllFiles(baseDir, "OverallSum");
//    		self.loadTiffFiles(file, parsers, geoInfo);
//    		System.out.println(parsers.size() + "_Finished!");
//	    }
	    System.out.println(parsers.size() + "_Finished!");
	    
	    geoInfo.width = 3600;
	    geoInfo.height = 2640;
		System.out.println("width: " + geoInfo.width + " height: " + geoInfo.height);
		double[] result = new double[geoInfo.width*geoInfo.height];
		self.computeHC(geoInfo.height, geoInfo.width, result, filePath, geoInfo);
//		save tiff file
		String outputfile = "/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/HierarchicalClst/TotalDistance/Result.tif";
		
		Driver driver = gdal.GetDriverByName("GTiff");
		Dataset dst_ds = driver.Create(outputfile, geoInfo.width, geoInfo.height, 1, gdalconst.GDT_Float32);
		dst_ds.SetGeoTransform(geoInfo.geoInfo);
		dst_ds.SetProjection(geoInfo.projRef);
		int writingResult = dst_ds.GetRasterBand(1).WriteRaster(0, 0, geoInfo.width, geoInfo.height, result);
		dst_ds.FlushCache();
		dst_ds.delete();
		System.out.println("Writing geotiff result is: " + writingResult);	
		
	}
	
	public void computeHC(int height, int width, double[] result, HashMap<String, File> parsers, GeotiffInfo geoInfo){
		int numOfProcessors = 16;
		ComputeHCThread[] ComputeHCThreadServices = new ComputeHCThread[numOfProcessors];
		Thread[] ComputeHCThreads = new Thread[numOfProcessors];
		for(int i=0; i<numOfProcessors; i++){
			int startHIndex = height/numOfProcessors*i;
			int endHIndex = height/numOfProcessors*(i+1);
			System.out.println(startHIndex + "," + endHIndex +","+i);
			ComputeHCThreadServices[i] = new ComputeHCThread(startHIndex, endHIndex, width, result, parsers, geoInfo);
			ComputeHCThreads[i] = new Thread(ComputeHCThreadServices[i]);
			ComputeHCThreads[i].start();
		}
		try{
			for(int i=0; i<numOfProcessors; i++){
				ComputeHCThreads[i].join();
				System.out.println(i+" is Finished!");
			}
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}

	public void loadTiffFiles(ArrayList<File> filePath, HashMap<String, Band> parsers, GeotiffInfo geoInfo){
		int numOfProcessors = filePath.size();
		LoadTiffsBand[] LoadTiffFilesServices = new LoadTiffsBand[numOfProcessors];
		Thread[] LoadTiffFilesThreads = new Thread[numOfProcessors];
//		int delta = files.size()/9;
		for(int i=0; i<numOfProcessors; i++){
			LoadTiffFilesServices[i] = new LoadTiffsBand(filePath.get(i).getName().replace(".tif", ""), filePath.get(i), parsers, geoInfo);
			LoadTiffFilesThreads[i] = new Thread(LoadTiffFilesServices[i]);
			LoadTiffFilesThreads[i].start();
		}
		try{
			for(int i=0; i<numOfProcessors; i++){
				LoadTiffFilesThreads[i].join();
			}
		}catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// module for getting all files within keywords, loop while
	public HashMap<String, File> getAllFiles(String directoryName, String keyword) {
			    File directory = new File(directoryName);
			    HashMap<String, File> result = new HashMap<String, File>();
			    // get all the files from a directory
			    File[] fList = directory.listFiles();
			    for (File file : fList) {
			    	String name = file.getName();
			        if (file.isFile() && name.endsWith(".tif") && name.contains(keyword)) {
			        	String key = file.getName().replace(".tif", "");
			        	if(!result.containsKey(key)) result.put(key, file);
			        }
			    }
			    if(result.size()==0) System.out.println("Cannot find the given file: " + keyword);
			    return result;
	}
	
}
