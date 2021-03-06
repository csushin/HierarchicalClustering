/*******************************************************************************
 * Copyright 2013 Lars Behnke
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package test.java.com.apporiented.algorithm.clustering;

import main.java.com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.Cluster;
import main.java.com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultClusteringAlgorithmTest {

	private double[][] distances;
	private String[] names;

	@Before
	public void setup() {
		distances = SampleClusterData.DISTANCES;
		names = SampleClusterData.NAMES;
	}

	@Test
	public void testClusteringAvgLink() {
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		Cluster c = alg.performClustering(distances, names, new AverageLinkageStrategy());
		Assert.assertNotNull(c);
	}

	@Test
	public void testDuplicateNames() {
//		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		try {
//			Cluster c = alg.performClustering(distances, SampleClusterData.NAMES_WITH_DUPLICATE,
//					new AverageLinkageStrategy());
			Assert.fail("Exception expected");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Duplicate names", e.getMessage());
		}
	}

}
