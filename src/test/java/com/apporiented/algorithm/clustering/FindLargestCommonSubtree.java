package test.java.com.apporiented.algorithm.clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.java.com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.Cluster;
import main.java.com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;


public class FindLargestCommonSubtree {
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
			
//			HashMap<String, String> historicalPts = new HashMap<String, String>();
//			HashMap<String, Integer> hashmap = new HashMap<String, Integer>();
			for(int h=this.startHIndex; h<this.endHIndex; h++){
				HashMap<String, Cluster> historicalPts = new HashMap<String, Cluster>();
				for(int w=0; w<this.width; w++){
					int index = (int) (h*this.width + w);
					boolean skip = true;
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
								if(!Double.isNaN(temp[0])){
									skip = false;
									dist[m][n] = temp[0];
								}
							 }
						}
					}
					if(!skip){
						int similarityDegree = 18*8;//18 models for the surrounding 8 grids.
						String key = Arrays.deepToString(dist);
						Cluster centerTree = null;
						if(historicalPts.containsKey(key)){
							centerTree = historicalPts.get(key);
						}
						else{
							centerTree = constructTree(dist, names);
							historicalPts.put(key, centerTree);
						}
						// in this data, the exception is that valid w and h would not on the boundary. Therefore I omit the exceptional situations
						for(int _w = w-1; _w<=w+1; _w++){
							for(int _h = h-1; _h<=h+1; _h++){
								if(_w!=w && _h!=h){
									String[] _names = new String[modelList.length];
									double[][] _dist = new double[modelList.length][modelList.length];
									constructDistMat(_w, _h, bandParsers, _names, _dist);
									String _key = Arrays.deepToString(_dist);
									Cluster surroundingTree = null;
									if(historicalPts.containsKey(_key)){
										surroundingTree = historicalPts.get(_key);
									}
									else{
										surroundingTree = constructTree(_dist, _names);
										historicalPts.put(key, surroundingTree);
									}
									for(int m=0; m<18 && similarityDegree>0; m++){
										String model = "O"+m;
										if(LocateLayerId(centerTree, model) != LocateLayerId(surroundingTree, model))
											similarityDegree--;
										else{
											if(!CompareNeighbors(model, centerTree, surroundingTree))
												similarityDegree--;
										}
									}
								}
							}
						}
						this.result[index] = similarityDegree/8.0;//get the average similarity to the surrounding grids.
//						System.out.println( w + " is Finihsed.");
						// for saving hashmap
						
					}
					else{
						this.result[index] = Double.NaN;
					}
				}
				System.out.println( h + " is Finihsed.");
			}
		}
		
	}
	
	
	public int LocateLayerId(Cluster root, String name){
		Cluster node = root.getLeafByName(name);
		int depth = 1;
		while(node.getParent()!=null){
			depth++;
			node = node.getParent();
		}
		return depth;
	}
	
	
	public boolean CompareNeighbors(String modelName, Cluster treeA, Cluster treeB){
		Cluster nodeA = treeA.getLeafByName(modelName);
		Cluster nodeB = treeB.getLeafByName(modelName);
		// get neighbors
		Cluster neighborA = getNeighbors(nodeA.getParent(), nodeA);
		Cluster neighborB = getNeighbors(nodeB.getParent(), nodeB);
			// compare types of leaf models
		ArrayList<String> leafA = new ArrayList();
		getLeaves(neighborA, leafA);
		ArrayList<String> leafB = new ArrayList();
		getLeaves(neighborB, leafB);
		if(leafA.size() != leafB.size())
			return false;
		else{
			for(int i=0; i<leafA.size(); i++){
				if(!leafB.contains(leafA.get(i)))
					return false;
				if(!leafA.contains(leafB.get(i)))
					return false;
			}
		}
		return true;
	}
	
	public Cluster getNeighbors(Cluster parent, Cluster knowns){
		for(Cluster child : parent.getChildren()){
			if(child.getName() != knowns.getName())
				return child;
		}
		return null;
	}
	
	public ArrayList<String> getLeaves(Cluster node, ArrayList<String> res){
		if(node.isLeaf()) res.add(node.getName());
		else{
			for (Cluster child : node.getChildren()) {
				getLeaves(child, res);
	        }
		}
		return res;
	}
	
	public void constructDistMat(int w, int h, HashMap<String, Band> bandParsers, String[] names, double[][] dist){
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
					if(!Double.isNaN(temp[0])){
						dist[m][n] = temp[0];
					}
				 }
			}
		}
	}
	
	public Cluster constructTree(double[][] dist, String[] names){
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		Cluster cluster = alg.performClustering(dist, names, new AverageLinkageStrategy());
		return cluster;
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
		FindLargestCommonSubtree self = new FindLargestCommonSubtree();
		GeotiffInfo geoInfo = self.new GeotiffInfo();
	    HashMap<String, File> filePath = new HashMap<String, File>();
	    String baseDir = "/work/asu/data/CalculationResults/tasmin_HIST/SimilarityResults/OverallSum/";
	    filePath = self.getAllFiles(baseDir, "OverallSum");
	    System.out.println(parsers.size() + "_Finished!");
	    
	    geoInfo.width = 3600;
	    geoInfo.height = 2640;
		System.out.println("width: " + geoInfo.width + " height: " + geoInfo.height);
		double[] result = new double[geoInfo.width*geoInfo.height];
		self.computeHC(geoInfo.height, geoInfo.width, result, filePath, geoInfo);
//		save tiff file
		String outputfile = "/work/asu/data/CalculationResults/tasmin_HIST/SimilarityResults/HierarchicalClst/TotalDistance/Similarity.tif";
		
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
		int numOfProcessors = 1;
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
