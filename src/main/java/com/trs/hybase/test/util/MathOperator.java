package com.trs.hybase.test.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;

public final class MathOperator {
	private MathOperator(){}

	public static <T extends Number> Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>> 
		statistics(List<T> vector){
		return statistics(vector, 0, vector.size());
	}
	/**
	 * 统计<code>List</code>中从下标<code>left</code>到下标<code>right</code>范围内的最大值<code>(max)</code>，<br>
	 * 最小值<code>(min)</code>,总和<code>(summary)</code>, 期望<code>(expectation)</code>, <br/>
	 * 方差<code>(variance)</code>, 中位数<code>(median)</code>和众数<code>(modes)</code><br>
	 * 并听过七元组<code>Tuple.Seven&ltA,B,C,D,E,F,G&gt</code>返回
	 * @param <T> 类型参数, 必须是Number的子类
	 * @param data 待处理列表
	 * @param left 左端
	 * @param right 右端(不包含此位置)
	 * @return <code>Tuple.Seventh&lt&gt(max, min, summary, expectation, variance, median, modes)</code><br>
	 * 当 left 或 right 指定端点非法时, 返回null<br>
	 */
	public static <T extends Number> Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>> 
		statistics(List<T> data, int left, int right){
		if(data == null || data.isEmpty())
			return null;
		if(left > right || left < 0 || left > data.size() || right < 0 || right > data.size())
			return null;
		List<T> list = data.subList(left, right);
		Collections.sort(list, (T value1, T value2) -> {
				if(value1.doubleValue() - value2.doubleValue() > 0)
					return 1;
				if(value1.doubleValue() - value2.doubleValue() < 0)
					return -1;
				return 0;
		});
		int size = list.size();
		/*中位数*/
		double median = 0.0;
		if(size < 2)
			median = list.get(0).doubleValue();
		if(size % 2 == 0)
			median = (list.get(size/2 - 1).doubleValue() + list.get(size/2).doubleValue())/2;
		else
			median = list.get(size/2).doubleValue();
		/*最值*/
		double min = list.get(0).doubleValue();
		double max = list.get(size-1).doubleValue();
		/*总和*/
		BigDecimal summary = new BigDecimal("0.0");
		BigDecimal summaryOfSqaureOfEachItem = new BigDecimal("0.0");
		BigDecimal current = null;
		/*众数列表*/
		List<Double> modes = new ArrayList<Double>();
		Map<Double, Integer> countMap = new HashMap<Double, Integer>();
		for(int i=0; i<size; i++) {
			current = new BigDecimal(list.get(i).doubleValue());
			summary = summary.add(current);
			summaryOfSqaureOfEachItem = summaryOfSqaureOfEachItem.add(current.multiply(current));
			countMap.put(list.get(i).doubleValue(), countMap.get(list.get(i).doubleValue())== null ?  
					1:(countMap.get(list.get(i).doubleValue())+1));
		}
		int modeFrequency = Integer.MIN_VALUE;
		Object[] counts = countMap.values().toArray();
		boolean haveMode = false;
		for(int i=0; i<counts.length-1; i++) {
			if(counts[i] != counts[i+1]) {
				haveMode = true;
				break;
			}
		}
		if(haveMode) {
			for(int i=0; i<counts.length; i++)
				if((int)counts[i] > modeFrequency)
					modeFrequency = (int)counts[i];
			for(Entry<Double, Integer> e: countMap.entrySet()) {
				if(e.getValue().intValue() == modeFrequency)
					modes.add(e.getKey());
			}
		}
		/*期望*/
		BigDecimal expectation = summary.divide(new BigDecimal(size), 6, BigDecimal.ROUND_HALF_UP);
		BigDecimal theSquareOfTheExpectation = expectation.multiply(expectation);
		BigDecimal theExpectationOfTheSquare = summaryOfSqaureOfEachItem.divide(new BigDecimal(size), 
				6, BigDecimal.ROUND_HALF_UP);
		/*方差*/
		BigDecimal variance = theExpectationOfTheSquare.subtract(theSquareOfTheExpectation);
		return new Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>>
		(max, min, summary, expectation, variance, median, modes);
	}
	

	public static Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>> 
		simpleStatistics(double... numbers){
		return statistics(numbers, 0, numbers.length);
	}
	public static Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>> 
		statistics(double[] array){
		return statistics(array, 0, array.length);
	}
	/**
	 * 统计<code>data</code>中从下标<code>left</code>到下标<code>right</code>范围内的最大值<code>(maxValue)</code>，<br/>
	 * 最小值<code>(minValue)</code>,总和<code>(summary)</code>, 期望<code>(expectation)</code>,<br/>
	 * 方差<code>(variance)</code>, 中位数<code>(median)</code>和众数<code>(modes)</code><br>
	 * 并通过七元组<code>FourTuple&ltA,B,C,D,E,F,G&gt</code>返回
	 * @param data
	 * @param left 指定的左端
	 * @param right 指定的右端
	 * @return <code>Tuple.Seven&lt&gt(max, min, summary, expectation, variance, median, modes)</code><br>
	 * 当left 或 right 指定端点非法时, 返回null<br>
	 */
	public static Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>> 
		statistics(double[] data, int left, int right){
		if(data == null || data.length == 0)
			return null;
		if(left > right || left < 0 || left > data.length - 1 || right < 0 || right > data.length - 1)
			return null;
		int size = right + 1 - left;
		double[] array = new double[size];
		System.arraycopy(data, left, array, 0, size);
		Arrays.sort(array);
		double median = 0.0;
		if(size % 2 == 0)
			median = (array[size/2 - 1] + array[size/2])/2;
		else
			median = array[size/2];
		double min = array[0];
		double max = array[size - 1];
		BigDecimal summary = new BigDecimal("0.0");
		BigDecimal summaryOfSqaureOfEachItem = new BigDecimal("0.0");
		BigDecimal current = null;
		List<Double> modes = new ArrayList<Double>();
		Map<Double, Integer> countMap = new HashMap<Double, Integer>();
		for(int i=0; i<size; i++) {
			current = new BigDecimal(array[i]);
			summary = summary.add(current);
			summaryOfSqaureOfEachItem = summaryOfSqaureOfEachItem.add(current.multiply(current));
			countMap.put(array[i], countMap.get(array[i])== null ?  1:(countMap.get(array[i])+1));
		}
		int modeFrequency = Integer.MIN_VALUE;
		Object[] counts = countMap.values().toArray();
		boolean haveMode = false;
		for(int i=0; i<counts.length-1; i++) {
			if(counts[i] != counts[i+1]) {
				haveMode = true;
				break;
			}
		}
		if(haveMode) {
			for(int i=0; i<counts.length; i++)
				if((int)counts[i] > modeFrequency)
					modeFrequency = (int)counts[i];
			for(Entry<Double, Integer> e: countMap.entrySet()) {
				if(e.getValue().intValue() == modeFrequency)
					modes.add(e.getKey());
			}
		}
		BigDecimal expectation = summary.divide(new BigDecimal(size), 6, RoundingMode.HALF_UP);
		BigDecimal theExpectationOfSquare = summaryOfSqaureOfEachItem.divide(new BigDecimal(size), 
				6, RoundingMode.HALF_UP);
		BigDecimal theSquareOfTheExpectation = expectation.multiply(expectation);
		BigDecimal variance = theExpectationOfSquare.subtract(theSquareOfTheExpectation);
		return new Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>>
		(max, min, summary, expectation, variance, median, modes);
	}
	
    /**
     * 0-1标准化
     * 
     * @param array 待处理序列
     * @return double[] 处理后的序列
     */
    public static double[] minMaxNormalization(double[] array){
    	double[] result = new double[array.length];
    	Tuple.Seven<Double, Double, BigDecimal, BigDecimal, BigDecimal, Double, List<Double>> 
    		statistic = statistics(array);
    	double max = statistic.first.doubleValue();
    	double min = statistic.second.doubleValue();
    	double difference = max - min;
    	for(int i=0; i<array.length; i++){
    		result[i] = (array[i] - min)/difference;
    	}
    	return result;
    }
    
    /**
     * 判断n是否为素数
     * 
     * @param n
     * @return boolean
     */
    public static boolean isPrimes(int n){
    	if(n < 2){
    		return false;
    	}
    	double sqrt = Math.sqrt(n);
    	for(int i=2; i<= sqrt; i++){
    		if(n % i == 0){
    			return false;
    		}
    	}
    	return true;
    }
    
    /**
     * 阶乘运算
     * 
     * @param n n的阶乘
     * @return BigDecimal 大数
     */
    public static BigDecimal factorial(int n){
    	if(n < 0){
    		throw new IllegalArgumentException("n is a minus");
    	}
    	BigDecimal result = new BigDecimal("1");
    	BigDecimal multNumber = new BigDecimal("1");
    	BigDecimal addValue = new BigDecimal("1");
    	if(n == 0 || n == 1){
    		return result;
    	}
    	for(int i=1; i<=n; i++){
    		result = result.multiply(multNumber);
    		multNumber = multNumber.add(addValue);
    	}
    	return result;
    }
    /**
     * 组合数
     * @param n
     * @param m
     * @return
     */
    public static BigDecimal C(int n, int m){
       if(n <= m)
    	   return new BigDecimal("1");
 	   return factorial(n).divide(factorial(m).multiply(factorial(n-m)), BigDecimal.ROUND_FLOOR);
    }
    /**
     * 排列数
     * @param n
     * @param m
     * @return
     */
    public static BigDecimal A(int n, int m){
    	if(n <= m)
    		return new BigDecimal("1");
    	return factorial(n).divide(factorial(n-m), BigDecimal.ROUND_FLOOR);
    }
    /**
     * 斯特林公式,阶乘结果的位数
     * @param n
     */
    public static int stirlingApproximation(int n){
    	if(n <= 2)
    		return 1;
    	return (int)Math.floor(Math.log10(2 * Math.PI * n) / 2 + n * Math.log10(n / Math.E)) + 1;
    }
    
    /**
     * 两个向量(终点)之间的闵可夫斯基距离. <br>元素类型必须为Number类的子类,两个向量的长度必须相等.
     * 
     * @param vector1 向量1
     * @param vector2 向量2
     * @param dimension 维度, dimension不小于1. <br>1为曼哈顿距离;2为欧几里得距离;数字越大越接近切比雪夫距离
     * @return Double 距离
     */
    public static <T extends Number> Double minkowskiDistance(List<T> vector1, List<T> vector2, int dimension){
    	if(vector1.size() != vector2.size())
    		throw new IllegalArgumentException("One vector's size don't equal the other's");
    	Double result = new Double(0.0);
    	if(dimension < 1)
    		dimension = 1;
    	for(int i=0, vectorSize=vector1.size(); i<vectorSize; i++)
    		result+=Math.pow((vector1.get(i).doubleValue() - vector2.get(i).doubleValue()), dimension);
    	result = Math.pow(result, (1.0/((double)dimension)));
    	return result;
    }
    
    /**
     * 求两个向量之间的余弦值,两个向量的长度必须相等
     * 
     * @param vector1
     * @param vector2
     * @return Double 余弦值
     */
    public static <T extends Number> Double cosine(List<T> vector1, List<T> vector2){
    	if(vector1.size() != vector2.size())
    		throw new IllegalArgumentException("One vector's size don't equal the other's");
    	Double numerator = new Double(0.0);
    	Double denominatorPart1 = new Double(0.0);
    	Double denominatorPart2 = new Double(0.0);
    	for(int i=0, length=vector1.size(); i<length; i++){
    		numerator += vector1.get(i).doubleValue() * vector2.get(i).doubleValue();
    		denominatorPart1 += Math.pow(vector1.get(i).doubleValue(), 2.0);
    		denominatorPart2 += Math.pow(vector2.get(i).doubleValue(), 2.0);
    	}
    	return numerator / (Math.pow(denominatorPart1, 0.5) * Math.pow(denominatorPart2, 0.5));
    }
    
    /**
     * 不大于n的最大质数
     * 如果n是一个质数,返回;
     * 如果n是一个合数,找比n小的最大质数
     * @param n
     * @return 最大质数,最小返回2
     */
    public static int findMaxPrime(int n){
    	for(int i=n; i>1; i--){
    		if(isPrimes(i))
    			return i;
    	}
    	return 2;
    }
    
    /**
     * 不小于n的最小质数
     * 如果n是一个质数,返回;
     * 如果n是一个合数,找比n大的最小质数
     * @param n 
     * @return
     */
    public static int findMinPrime(int n){
    	for(int i=n;i<=Integer.MAX_VALUE;i++){
    		if(isPrimes(i))
    			return i;
    	}
    	return Integer.MAX_VALUE;
    }
    
    /**
     * 求两个正整数的最大公约数
     */
    public static int greatCommonDiviser(int a, int b){
    	if(a <= 0 || b <= 0)
    		throw new IllegalArgumentException("Parameter must greater than 0!");
    	int r = 0;
    	while(b > 0){
    		r = a % b;
    		a = b;
    		b = r;
    	}
    	return a;
    }
    
    /**
     * 求两个正整数的最小公倍数
     */
    public static long leastCommonMultiple(int a, int b){
    	return ((long)a * (long)b / greatCommonDiviser(a, b));
    }
    
    /**
     * 返回小于n的所有质数
     * 
     * @param n 自然数n, n不能小于2，否则返回空序列；
     * @return <b><code>List</code></b> 质数序列
     */
    public static List<Integer> getPrimesList(int n){
    	List<Integer> primesList = new ArrayList<Integer>();
    	if(n < 2)
    		return primesList;
    	primesList.add(2);
    	if(n == 2)
    		return primesList;
    	boolean isPrimes = false;
    	for(int i=3; i<n; i+=2){
    		for(int j=0,size=primesList.size(); j<size; j++){
    			if(i % primesList.get(j) == 0){
    				isPrimes = false;
    				break;
    			}
    			isPrimes = true;
    		}
    		if(isPrimes)
    			primesList.add(i);
    	}
    	return primesList;
    }
    /**
     * 取前n位质数
     * @param count 取出的数量,小于1时返回空表
     * @return List&ltInteger&gt质数表
     */
    public static List<Integer> getPreviousNPrimes(int count){
    	if(count < 1)
    		return new ArrayList<Integer>();
    	if(count == 1){
    		List<Integer> primes = new ArrayList<Integer>(1);
        	primes.add(2);
        	return primes;
    	}
    	List<Integer> primes = new ArrayList<Integer>(count);
    	primes.add(2);
    	boolean isPrimes = false;
    	for(int i=3; i<Integer.MAX_VALUE; i+=2){
    		for(int j=0,size=primes.size(); j<size; j++){
    			if(i % primes.get(j) == 0){
    				isPrimes = false;
    				break;
    			}
    			isPrimes = true;
    		}
    		if(isPrimes)
    			primes.add(i);
    		if(primes.size() >= count)
    			break;
    	}
    	return primes;
    }
    /**
     * 调和级数前n项和
     * @param n 第n项
     * @return 调和级数的前n项和
     */
    public static double sumOfHarmoric(int n){
    	double sum = 0.0;
    	for(int i=1; i<=n; i++)
    		sum += 1.0 / i;
    	return sum;
    }
    /**
     * 交错调和级数前n项和
     * @param n 第n项
     * @return 前n项和
     */
    public static double sumOfAlternatingHarmonic(int n){
    	double sum = 0.0;
    	for(int i=1; i<=n; i++)
    		sum += Math.pow(-1, i+1) * (1.0 / i);
    	return sum;
    }
    /**
     * 等差数列前n项和
     * @param initial 首项
     * @param difference 公差
     * @param count 项数
     * @return 前n项和
     */
    public static long sumOfArithmeticProgression(int initial, int difference, int count){
    	if(count <= 1)
    		return initial;
    	return count * initial + (count * (count - 1) / 2) * difference;
    }
    /**
     * 等差数列前n项和
     * @param initial 首项
     * @param last 末项
     * @param count 项数
     * @return
     */
    public static long sumOfArithmeticProgressionWithLast(int initial, int last, int count){
    	if(count <= 1)
    		return initial;
    	return (initial + last) * count / 2;
    }
    /**
     * 等比数列前n项和
     * @param initial 首项,为0时直接返回首项
     * @param ratio 公比,为0时直接返回首项
     * @param count 项数,小于1时直接返回首项
     * @return 前n项和
     */
    public static double sumOfGeometricProgression(int initial, int ratio, int count){
    	if(initial == 0 || count < 1 || ratio == 0)
    		return initial;
    	if(count == 1)
    		return initial;
    	if(ratio == 1)
    		return initial * count;
    	return initial * (1 - Math.pow(ratio, count)) / (1 - ratio);
    }
    
    private final static double PRECISE_STANDARD = 1e-15;
    /**
     * 牛顿迭代法求开根号
     * @param n 被开根号的数
     * @return 正根植
     */
    public static double sqrt(double n){
    	if(n < 0)
    		return Double.MIN_VALUE;
    	/*
    	 * 求n的m次根本质上是在求 f(x) = x^m - n，然后令f(x)=0 
    	 * 
    	 * 设f(x)在某区间内m阶可导, 根据泰勒公式, f(x) ~ f(x0) + f`(x0)(x - x0) + O(f``````````(x))
    	 * 
    	 * 取前两项构成一个新函数φ(x) = f(x0) + f'(x0)(x - x0)
    	 * 
    	 * 令φ(x) = 0, 则有 0 = f(x0) + f`(x0)(x - x0),  反解x = x0 - f(x0)/f`(x0)
    	 * 
    	 * 初始的x0是一个猜测值,不一定就是最终的解,也许距离真正的解很远,如果没有满足进度要求,就按照反解出来的x反复迭代
    	 * 
    	 * 如f(x) = x^2 - n
    	 * 
    	 * f(x) ~ (x0^2 - n) + 2*(x - x0) + 2
    	 * 
    	 * 令φ(x) = (x0^2 - n) + 2*x0*(x - x0)  = 0
    	 * 
    	 * 则有 x0^2 - n + 2*x0*x - x0^2 = 0
    	 * 
    	 * 整理后 x = (x0 + n/x0) / 2
    	 * 
    	 * 每次迭代, 将上一轮得出的x视为新一轮迭代的x0,求出新一轮迭代的x, 反复
    	 */
    	double x1 = n;
    	double x2 = n/2;
    	while(Math.abs(x1 - x2) > PRECISE_STANDARD){
    		x1 = x2;
    		x2 = (x1 + n/x1) / 2;
    	}
    	return x1;
    }
    /**
     * 返回斐波那契数列前n项构成的表
     * @param n 
     * @return
     */
    public static List<BigDecimal> fibonacciList(int n){
    	List<BigDecimal> result = new ArrayList<BigDecimal>();
    	if(n <= 0)
    		return result;
    	result.add(0,new BigDecimal(1));
    	if(n == 1)
    		return result;
    	result.add(1,new BigDecimal(1));
    	if(n == 2)
    		return result;
    	for(int i=2; i<n; i++)
    		result.add(i, result.get(i-1).add(result.get(i-2)));
    	return result;
    }
   /*泰勒公式用的迭代次数, 1024 * 1024 * 8 = 8388608, 方便快速平方算法*/
   private final static int ITERATIONS = 8388608;
   /**
    * ln(x + 1)
    * @param x
    * @return
    */
   public static double naturalLogarithmAddOne(double x){
	   if(x <= -1.0 || x > 1.0)
		   return Double.NaN;
	   double result = 0.0;
	   double constParameter = 1.0;
	   for(int i=1; i<=ITERATIONS; i++){
		   if(i % 2 == 1)
			   constParameter = 1.0;
		   else
			   constParameter = -1.0;
		   result += constParameter/i * Math.pow(x, i);
	   }
	   return result;
   }
   /**
    * 两个向量的内积
    * @param vector1
    * @param vector2
    * @return
    */
   public static <T extends Number> double innerProduct(List<T> vector1, List<T>vector2){
	   if(vector1 == null || vector1.isEmpty() || vector2 == null || vector2.isEmpty() || vector1.size() != vector2.size())
		   return Double.NaN;
	   double result = 0.0;
	   for(int i=0, size=vector1.size(); i<size; i++)
		   result += vector1.get(i).doubleValue() * vector2.get(i).doubleValue();
	   return result;
   }
   /**
    * 向量叉积, 要求向量维数必须是3
    * @param former
    * @param latter
    * @return
    */
   public static <T extends Number> List<Double> crossProduct(List<T> former, List<T> latter){
	   if(former.size() != 3 || latter.size() != 3)
		   throw new IllegalArgumentException("please check your vectors length!");
	   List<Double> result = new ArrayList<Double>(3);
	   result.add(0, former.get(1).doubleValue() * latter.get(2).doubleValue() - 
			   former.get(2).doubleValue() * latter.get(1).doubleValue());
	   result.add(1, former.get(2).doubleValue() * latter.get(0).doubleValue() - 
			   former.get(0).doubleValue() * latter.get(2).doubleValue());
	   result.add(2, former.get(0).doubleValue() * latter.get(1).doubleValue() - 
			   former.get(1).doubleValue() * latter.get(0).doubleValue());
	   return result;
   }
   /**
    * 向量加法
    * @param vector1
    * @param vector2
    * @return
    */
   public static <T extends Number>List<Double> vectorAdd(List<T> vector1, List<T> vector2){
	   if(vector1 == null || vector1.isEmpty() || vector2 == null || vector2.isEmpty() || vector1.size() != vector2.size())
		   return null;
	   List<Double> result = new ArrayList<Double>();
	   for(int i=0,size=vector1.size(); i<size; i++)
		   result.add(i, vector1.get(i).doubleValue() + vector2.get(i).doubleValue());
	   return result;
   }
   /**
    * 向量减法
    * @param vector1
    * @param vector2
    * @return
    */
   public static <T extends Number> List<Double> vectorMinus(List<T> vector1, List<T> vector2){
	   if(vector1 == null || vector1.isEmpty() || vector2 == null || vector2.isEmpty() || vector1.size() != vector2.size())
		   return null;
	   List<Double> result = new ArrayList<Double>();
	   for(int i=0,size=vector1.size(); i<size; i++)
		   result.add(i, vector1.get(i).doubleValue() - vector2.get(i).doubleValue());
	   return result;
   }
   /**
    * 向量数乘
    * @param constant 常数
    * @param vector 向量
    * @return
    */
   public static List<Double> vectorMultiplyingConstant(double constant, List<? extends Number> vector){
	   if(vector == null || vector.isEmpty())
		   return null;
	   List<Double> result = new ArrayList<Double>();
	   for(int i=0,size=vector.size(); i<size; i++)
		   result.add(i, vector.get(i).doubleValue() * constant);
	   return result;
	}
   
   /**
    * 对<code>&ltlist&gt</code>按value做次数统计
    * @param list
    * @return <code>Map&ltT key, Long value&gt</code> 其中key表示list中的值, value表示list中出现的次数
    */
   public static <T> Map<T,Long> category(List<T> list){
   	Map<T, Long> result = new HashMap<T, Long>();
   	T currentKey = null;
   	for(int i=0,size=list.size(); i<size; i++){
   		currentKey = list.get(i);
   		if(result.containsKey(currentKey))
   			result.put(currentKey, result.get(currentKey)+1);
   		else
   			result.put(currentKey, 1L);
   	}
   	return result;
   }
   /**
    * 以a为底b的对数
    * 
    * @param a
    * @param b
    * @return
    */
   public static double baseNLogarithmic(double a, double b){
	   return Math.log10(b) / Math.log10(a);
   }
}
