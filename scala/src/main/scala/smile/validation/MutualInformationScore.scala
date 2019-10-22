/*******************************************************************************
 * Copyright (c) 2010-2019 Haifeng Li
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/

package smile.validation

import scala.math.{max, log, sqrt}

 /**
   * Mutual Information score between two clusterings with optional normalization.
   * Normalized Mutual Information is an normalization of the Mutual Information
	 * score to scale the results between 0 (no mutual information) and 1 (perfect correlation).
   *
   * Note that this measure is not adjusted for chance. This metric is independent of the
	 * absolute values of the labels: a permutation of the class or cluster label values
	 * won’t change the score value in any way.
	 *
   * @param normalization "sqrt" normalized by sqrt(H(labels_true) * H(labels_pred))
   *                      "max"  normalized by max(H(labels_true) * H(labels_pred))
   *                      ""     no normalization
   *
	 * @author Beck Gaël
   */
class MutualInformationScore(normalization: String = "sqrt") extends ClusterMeasure {
	override def measure(x: Array[Int], y: Array[Int]) = {
		val (mi, hu, hv) = MutualInformationScore(x, y)
		normalization match {
			case "mi" => mi // no normalization
			case "sqrt" => mi / sqrt(hu * hv)
			case "max" => mi / max(hu, hv)
			case _ => throw new UnsupportedOperationException("Unsupported normalization type: " + normalization)
		}
	}
}

object MutualInformationScore {
	def apply(x: Array[Int], y:Array[Int]): (Double, Double, Double) = {
		require( x.size == y.size )
		val n = x.size
		val xx = prepareList(x)
		val yy = prepareList(y)
		val maxX = xx.max
		val maxY = yy.max

		val maxOneIndices = (0 to maxX).toArray
		val maxTwoIndices = (0 to maxY).toArray

		val count = Array.fill(maxX + 1)(Array.fill(maxY + 1)(0D))
		for( i <- xx.indices ) count(xx(i))(yy(i)) += 1D

		val ai = new Array[Double](maxX + 1)
		val bj = new Array[Double](maxY + 1)

		maxOneIndices.foreach( m => maxTwoIndices.foreach( l => ai(m) += count(m)(l) ) )
		maxTwoIndices.foreach( m => maxOneIndices.foreach( l => bj(m) += count(l)(m) ) )


		val nN = ai.reduce(_ + _)
		// Entropy for input data
		var hu = 0D
		ai.foreach( v => { val c = v / nN; if( c > 0 ) hu -= c * log(c) } )
		// Entropy for true labeled data
		var hv = 0D
		bj.foreach( v => { val c = v / nN; if( c > 0) hv -= c * log(c) } )

		var huStrichV = 0D
		maxOneIndices.foreach( i => maxTwoIndices.foreach( j => if( count(i)(j) > 0 ) huStrichV -= count(i)(j) / nN * log( (count(i)(j)) / bj(j) ) ) )

		val mi = hu - huStrichV
		(mi, hu, hv)
	}

	/**
		* Normalize Sequences in order to prevent construction of a to big 'count' matrix
		* Ex: [4,5,6,6] -> [0,1,2,2]
		**/
	private def prepareList(x: Array[Int]): Array[Int] = {
		val indexedValuesMap = x.distinct.zipWithIndex.toMap
		x.map(indexedValuesMap)
	}
}