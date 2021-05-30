package com.trs.hybase.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringOperator {
	private final static class Key{
		private Key() {}
		@SuppressWarnings("unused")
		private final static String UNICODE_BIG_UNMARKED = "UnicodeBigUnmarked";
	    @SuppressWarnings("unused")
		private final static String UTF8 = "UTF-8";
	    @SuppressWarnings("unused")
		private final static String GBK = "GBK";
	    private final static String CONTAINS_SPACE_LINE = "contains.space.line";
	    private final static String UP_LEFT = "UP_LEFT";
	    private final static String UP = "UP";
	    private final static String LEFT = "LEFT";
	    private final static String FALSE = "false";
	    private final static String EMPTY_STRING = "";
	}
	/*
	 * 防止类被实例化
	 */
	private StringOperator(){}
	
	private final static double THREE_WORD_NAME_CHANCE = 0.5;
	private final static String[] FAMILY_NAME_SET = new String[]{
		"赵","钱","孙","李","周","吴","郑","王","冯","陈",
		"诸","卫","蒋","沈","韩","杨","朱","秦","尤","许",
		"何","吕","施","张","孔","曹","严","华","金","魏",
		"陶","姜","戚","谢","邹","喻","柏","水","窦","章",
		"云","苏","潘","葛","奚","范","彭","郎","鲁","韦",
		"昌","马","苗","凤","花","方","俞","任","袁","柳",
		"酆","鲍","史","唐","费","廉","岑","薛","雷","贺",
		"倪","汤","滕","殷","罗","毕","郝","邬","安","常",
		"乐","于","时","傅","皮","卡","齐","康","伍","余",
		"元","卜","顾","孟","平","黄","和","穆","萧","尹",
		"姚","邵","堪","汪","祁","毛","禹","狄","米","贝",
		"明","臧","计","伏","成","戴","谈","宋","茅","庞",
		"熊","纪","舒","屈","项","祝","董","粱","杜","阮",
		"蓝","闵","席","季","麻","强","贾","路","娄","危",
		"江","童","颜","郭","梅","盛","林","刁","钟","徐",
		"邱","骆","高","夏","蔡","田","樊","胡","凌","霍",
		"虞","万","支","柯","咎","管","卢","莫","司马","上官",
		"欧阳","夏侯","诸葛","东方","皇甫","尉迟","公羊","濮阳","淳于","单于",
		"太叔","公孙","轩辕","令狐","钟离","宇文","长孙","慕容","鲜于","司徒",
		"端木","拓拔","百里","东郭","呼延","左丘","南宫","汝鄢","司寇","归海"
	};
	
	private final static String[] NAME_SET = new String[]{
		"伟","勇","军","磊","涛","斌","强","鹏","杰","峰","超","波","辉","刚","健",
		"明","亮","俊","飞","凯","浩","华","平","鑫","毅","林","洋","宇","敏","宁",
		"建","兵","旭","雷","锋","彬","龙","翔","阳","剑","东","博","威","海","巍",
		"晨","炜","帅","岩","江","松","文","云","力","成","琦","进","昊","宏","欣",
		"坤","冰","锐","震","楠","佳","忠","庆","杨","新","骏","君","栋","青","帆",
		"静","荣","立","虎","哲","晖","玮","瑞","光","钢","丹","坚","振","晓","祥",
		"良","春","晶","猛","星","政","智","琪","永","迪","冬","琳","胜","康","彪",
		"乐","诚","志","维","卫","睿","捷","群","森","洪","扬","科","奇","铭","航",
		"利","鸣","恒","源","聪","凡","颖","欢","昕","武","雄","洁","川","清","义",
		"滨","皓","达","民","跃","越","兴","正","靖","曦","璐","挺","淼","泉","程",
		"韬","冲","硕","远","昆","瑜","晔","煜","红","权","征","雨","野","慧","萌",
		"山","丰","珂","彤","悦","朋","钧","彦","然","枫","嘉","峥","寅","烨","铮",
		"卓","畅","钊","金","可","昱","爽","盛","路","晋","谦","克","方","闯","耀",
		"奎","一","晟","勤","豪","安","尧","全","琛","腾","队","鸿","玉","泽","凌",
		"渊","蕾","广","顺","莹","英","峻","攀","宾","驰","燕","霖","喆","椒","国",
		"恺","潇","琨","轶","芳","吉","亚","梁","焱","侃","臻","嵩","岳","炯","艳",
		"宝","岗","斐","啸","元","辰","萍","柯","羽","培","通","天","麟","晗","菲",
		"雪","铁","贺","钰","戈","灿","琼","锦","生","原","洲","炎","丽","勋","奕",
		"艺","中","德","轩","京","标","旺","南","黎","禹","莉","蔚","总","益","祺",
		"骥","沛","汉","真","非","鹤","升","蒙","城","钦","锴","骁","壮","罡","键",
		"瑶","虹","石","展","翼","为","灏","玲","放","娜","露","赞","娟","倩","懿",
		"劲","婷","策","魁","霄","冉","敬","卿","均","治","迅","臣","桦","镇","骞",
		"河","希","瑾","鹰","舟","丁","涵","弘","纲","泳","理","福","俭","乾","纯",
		"双","屹","涌","根","怡","果","田","岭","昭","飚","勃","嵘","熙","贤","申",
		"琰","宽","鼎","滔","昌","璞","逸","贵","喜","昂","柳","韶","瑛","伦","茂",
		"景","柱","岚","实","珏","霞","园","学","惠","衡","风","玺","赫","桐","寒",
		"圣","陈","旋","礼","霆","月","侠","密","堃","富","薇","仁","浪","津","垒",
		"齐","炼","瀚","泓","灵","朝","夏","严","意","银","璇","鲲","易","行","品",
		"垄","靓","苏","澄","赛","思","旗","淳","雯","继","友","和","革","延","能",
		"菁","叶","隽","烽","昶","笑","裕","鲁","铎","昀","骅","高","翀","润","熠",
		"锟","望","卡","微","拓","名","秋","冶","雁","开","定","想","舒","庚","蓉",
		"牧","重","孟","澎","信","郁","珉","钟","盼","恩","周","潮","季","烈","魏",
		"奔","承","玎","来","桥","尚","增","婧","茜","前","琴","麒","竞","童","舜",
		"会","柏","冠","佩","游","珊","融","满","添","咏","响","珩","杉","韧","梅",
		"乔","同","梦","树","杭","念","遥","苗","胤","榕","耿","崇","湘","里","疆",
		"旻","启","烁","楷","才","仲","隆","媛","晴","章","舰","璟","桔","李","影",
		"亭","珺","言","笛","弛","营","宪","渝","发","逊","运","豹","翊","研","登",
		"炳","蕊","鉴","妍","焰","颂","闻","桢","镭","特","曙","盟","贝","千","保",
		"功","竹","印","玥","夭","冀","阔","圆","湛","澍","争","众","肖","祯","默",
		"珍","煌","余","准","忱","宸","普","韦","舸","创","芸","彭","泰","心","廷"
	};
	
	private static final int[][] RANGE = new int[][]{
		new int[]{607649792, 608174079},
		new int[]{1038614528, 1039007743},
		new int[]{1783627776, 1784676351},
		new int[]{2035023872, 2035154943},
		new int[]{2078801920, 2079064063},
		new int[]{-1950089216, -1948778497},
		new int[]{-1425539072, -1425014785},
		new int[]{-1236271104, -1235419137},
		new int[]{-770113536, -768606209},
		new int[]{-569376768, -564133889}
	};
	
	private static final String[] UPPER_LETTER_SET = new String[]{
		"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"
	};
	private static final String[] LOWER_LETTER_SET = new String[]{
		"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"
	};
	private static final String[] LETTER_SET = new String[]{
		"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
		"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"
	};
	private static final String[] NUMBER_SET = new String[]{
		"0","1","2","3","4","5","6","7","8","9"
	};
	private static final String[] NUMBER_AND_UPPER_LETTER_SET = new String[]{
		"0","1","2","3","4","5","6","7","8","9",
		"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"
	};
	private static final String[] NUMBER_AND_LOWER_LETTER_SET = new String[]{
		"0","1","2","3","4","5","6","7","8","9",
		"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"
	};
	private static final String[] MIXED_CHARACTER_SET = new String[]{
		"0","1","2","3","4","5","6","7","8","9",
		"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
		"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"
	};
	
	public final static String[] COMMON_SYMBOLS_CHARACTERS = new String[]{
		",", ".", "/", "?", ";", "'", "\\", ":", "\"", "|", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "=", "_", "+", "~", "`","{","}","[","]",
		"，", "。", "？", "；", "：","‘","“","”", "、", "【", "】","·", "！", "￥", "…", "（", "）", "—"
	};
	/**
	 * 判断文本是否非空
	 * 
	 * @param text 待测文本
	 * @return boolean 是否非空
	 */
	public static boolean isNotEmpty(String text){  
        boolean result = false;
        if((text != null) && (!Key.EMPTY_STRING.equals(text)))
        	result = true;
        return result;
    }
	/** 
     * 检验字符串是否匹配指定的正则表达式 
     *  
     * @param regex 正则表达式 
     * @param input 字符串 
     * @return boolean true-匹配 false-不匹配 
     */  
    public static boolean validByRegex(String regex, String input)  {  
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);  
        Matcher regexMatcher = p.matcher(input);  
        return regexMatcher.find();  
    }
    
    /*一些常用的正则表达式*/
    public final static String REGEX_URL = "(http://|https://)?([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
    public final static String REGEX_INTEGER = "^[-+]{0,1}\\d*$";
    public final static String REGEX_ZIPCODE = "^[0-9]{6}$";
    public final static String REGEX_EMAIL = "([\\w[.-]]+)(@)([\\w[.-]]+\\.[\\w[.-]]+)";
    public final static String REGEX_MOBILE = "((\\()?(\\+)?(86)?1[3|5|8][0-9]{9}(\\))?$)|((\\()?(\\+)?(86)?01[3|5|8][0-9]{9}(\\))?$)";
    public final static String REGEX_PASSWD = "([A-Za-z0-9_]{4,16})";
  
    /** 
     * 是否为数字 
     *  
     * @param text 要验证的文本 
     * @return boolean true-是 false-否
     * @throws Exception text为Null或空串时, 抛出异常
     */  
    public static boolean isNumber(String text){  
        if (!isNotEmpty(text))
            throw new NullPointerException("Text is Null Or Empty");  
        String numberStr = "0123456789";  
        for (int i = 0; i < text.length(); i++){  
            char c = text.charAt(i);  
            if (numberStr.indexOf(String.valueOf(c)) == -1)
                return false;  
        }  
        return true;  
    }
    /** 
     * 电话号码格式简单校验，格式为：数字-数字 
     *  
     * @param phone 电话号码 
     * @return boolean true-是 false-否 
     * @throws Exception 空串或Null抛出异常
     */  
    public static boolean isPhone(String phone) throws Exception{  
        boolean isPhone = false;  
        int index = phone.indexOf("-");  
        if (index > 0 && index != phone.length() - 1){  
            String phoneNum = phone.substring(0, index) + phone.substring(index + 1);  
            if (isNumber(phoneNum))
                isPhone = true;  
        }  
        return isPhone;  
    }
    /** 
     * 转换为大写,只对其中的英文字母有效 
     *  
     * @param str 字符串 
     * @return String 大写字符串 
     * @throws Exception str为Null或空串时
     */  
    public static String upperCase(String str){  
        if (!isNotEmpty(str)){  
            return Key.EMPTY_STRING;
        }
        char c;
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<str.length(); i++){
        	c = str.charAt(i);
        	if(c >= 'a' && c <= 'z'){
        		sb.append((char)(c-32));
        	}else{
        		sb.append(c);
        	}
        }
        return sb.toString();
    } 
    /** 
     * 转换为小写,只对其中的英文字母有效  
     *  
     * @param str 字符串 
     * @return String 小写字符串 
     * @throws Exception str为Null或空串时
     */  
    public static String lowerCase(String str){  
    	if (!isNotEmpty(str)){  
            return Key.EMPTY_STRING;
        }
    	char c;
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<str.length(); i++){
         	c = str.charAt(i);
         	if(c >= 'A' && c <= 'Z'){
         		sb.append((char)(c+32));
         	}else{
         		sb.append(c);
         	}
         }
         return sb.toString();
    }
    /**
     * 将字符串中的英文和数字全角变半角
     * 
     * @param str 源字符串
     * @return
     */
    public static String toHalfAngle(String str){
    	if (!isNotEmpty(str))
            return Key.EMPTY_STRING;
    	StringBuilder sb = new StringBuilder();
    	char c;int v;
    	for(int i=0; i<str.length(); i++){
    		c = str.charAt(i);v = (int)c;
    		if(
    			(v >= 65296 && v <= 65305) ||
    			(v >= 65313 && v <= 65338) ||
    			(v >= 65345 && v <= 65370) ){
    			sb.append((char)(c-65248));
    		}else{
    			sb.append(c);
    		}
    	}
    	return sb.toString();
    }
    /**
     * 将字符串中的英文和数字半角变全角
     * 
     * @param str 源字符串
     * @return
     */
    public static String toFullAngle(String str){
    	if (!isNotEmpty(str))
            return Key.EMPTY_STRING;
    	StringBuilder sb = new StringBuilder();
    	char c;int v;
    	for(int i=0; i<str.length(); i++){
    		c = str.charAt(i);v = (int)c;
    		if(
    			(v >= 48 && v <= 57) ||
    			(v >= 65 && v <= 90) ||
    			(v >= 97 && v <= 122) ){
    			sb.append((char)(c+65248));
    		}else{
    			sb.append(c);
    		}
    	}
    	return sb.toString();
    }

    /** 
     * 把字符串由一种编码转换为另一种编码
     *  
     * @param string 字符串 
     * @param from 源编码 
     * @param to 目标编码 
     * @return String 编码后的字符串
     * @throws Exception 转换失败时抛出异常 
     */  
    public static String encodingString(String string, String from, String to)throws Exception{  
        return new String(string.getBytes(from), to);  
    }
    
    /**
     * 生成一段随机的字符串
     * @param length 长度
     * @param characterType 字符类型, 可选值English, Number和Mixed
     * @param letterType 英文字符的大小写, 仅当字符类型为English和Mixed有效,可选值Upper, Lower和Mixed
     * @return String
     */
    public static String getRandomString(int length, CharacterType characterType, LetterType letterType){
    	length = Math.abs(length);
    	String[] characterSet = null;
    	StringBuilder stringBuilder = new StringBuilder();
    	if(characterType == CharacterType.Mixed && letterType == LetterType.Mixed){
    		characterSet = StringOperator.MIXED_CHARACTER_SET;
    	}else if(characterType == CharacterType.English && letterType == LetterType.Mixed){
    		characterSet = StringOperator.LETTER_SET;
    	}else if(characterType == CharacterType.Number){
    		characterSet = StringOperator.NUMBER_SET;
    	}else if(characterType == CharacterType.Mixed && letterType == LetterType.Lower){
    		characterSet = StringOperator.NUMBER_AND_LOWER_LETTER_SET;
    	}else if(characterType == CharacterType.Mixed && letterType == LetterType.Upper){
    		characterSet = StringOperator.NUMBER_AND_UPPER_LETTER_SET;
    	}else if(characterType == CharacterType.English && letterType == LetterType.Lower){
    		characterSet = StringOperator.LOWER_LETTER_SET;
    	}else{
    		characterSet = StringOperator.UPPER_LETTER_SET;
    	}
    	Random random = new Random();
    	for(int i=0; i<length; i++){
    		stringBuilder.append(characterSet[random.nextInt(characterSet.length)]);
    	}
    	return stringBuilder.toString();
    }
    
    public enum CharacterType{
    	English, Number, Mixed
    }
    public enum LetterType{
    	Upper, Lower, Mixed
    }
    
    /**
     * 从库里获取一个随机人名
     * 
     * @return String 人名
     */
    public static String getAName(){
    	StringBuilder sb = new StringBuilder();
    	Random random = new Random();
    	sb.append(FAMILY_NAME_SET[random.nextInt(FAMILY_NAME_SET.length)]);
    	sb.append(NAME_SET[random.nextInt(NAME_SET.length)]);
    	double chance = Math.random();
    	if(chance < THREE_WORD_NAME_CHANCE)
    		sb.append(NAME_SET[random.nextInt(NAME_SET.length)]);
    	return sb.toString();
    }
    
    /**
     * 将字符串转换为二进制码列表
     * @param string 源字符串
     * @return <b><code>List</code></b> 列表,表内的每个元素都是一个字符的二进制编码
     */
    public static List<String> stringToBinaryList(String string){
    	List<String> stringBinaryList = new LinkedList<String>();
    	for(int i=0,length=string.length(); i<length; i++){
    		stringBinaryList.add(Integer.toBinaryString(string.charAt(i)));
    	}
    	return stringBinaryList;
    }
    /**
     * 将二进制编码列表转换为字符串
     * @param binaryList 列表，表内的每个元素都是一个字符的二进制编码
     * @return 对应的字符串
     */
    public static String binaryListToString(List<String> binaryList){
    	StringBuilder sb = new StringBuilder();
    	for(int i=0,size=binaryList.size(); i<size; i++){
    		sb.append((char)(Integer.valueOf(binaryList.get(i),2).intValue()));
    	}
    	return sb.toString();
    }
    
    /**
     * 返回一个随机的IP
     * @return
     */
    public static String getARandomIp(){
    	int index = (int)(Math.random() * RANGE.length);
    	int ipNumber = RANGE[index][0] + (int)(Math.random() * (RANGE[index][1] - RANGE[index][0]));
    	int[] ipPart = new int[4];
    	StringBuilder sb = new StringBuilder();
    	ipPart[0] = (int)((ipNumber >> 24) & 0xff);
    	ipPart[1] = (int)((ipNumber >> 16) & 0xff);
    	ipPart[2] = (int)((ipNumber >> 8 ) & 0xff);
    	ipPart[3] = (int)((ipNumber & 0xff));
    	sb.append(ipPart[0]+"."+ipPart[1]+"."+ipPart[2]+"."+ipPart[3]);
    	return sb.toString();
    }
    
    
    private final static String DEFAULT_ENCODING = "UTF-8";
    /**
     * 将一串文本按行读取, 然后将每一行文本当做列表的每一行, 最后返回列表
     * @param str 源文本
     * @param encoding 编码
     * @param param 可选参数<br>
     * <b><code>contains.space.line</code></b> 是否保留空行 true-是|false-否, 默认false
     * @return <code>&ltList&gt</code> 文本列表
     * @throws IOException 读取文本串发生错误时
     */
    public static LinkedList<String> asLines(String str, String encoding, Map<String,String> param)throws IOException{
    	LinkedList<String> result = new LinkedList<String>();
    	if(str == null || str.isEmpty())
    		return result;
    	if(encoding == null || Key.EMPTY_STRING.equals(encoding))
    		encoding = DEFAULT_ENCODING;
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(str.getBytes(Charset.forName(encoding))), Charset.forName(encoding)));
    	String current;
    	boolean containsSpaceLine = Boolean.valueOf(MapOperator.safetyGet(param, Key.CONTAINS_SPACE_LINE, Key.FALSE));
    	while((current = bufferedReader.readLine())!=null){
    		if(current.isEmpty() && containsSpaceLine){
    			result.add(current);
    			continue;
    		}
    		result.add(current.trim());
    	}
    	bufferedReader.close();
    	return result;
    }
    
    public interface ILineExecutor {
		/**
		 * 单行文本过滤器,过滤时仅以此方法的返回值作为依据;<br>
		 * 空串等特殊情况是否保留需要用户自行判断;
		 * @param currentLine
		 * @return
		 */
		boolean accept(String currentLine);
		/**
		 * 对过滤后的单行文本的处理, 由用户实现; 
		 * 如果返回null, 不会添加此行;
		 * 换行符需要用户自行添加
		 * @param currentLine
		 * @return
		 */
		String process(String currentLine);
	}
    /**
     * 将一串文本按行读取, 然后将每一行文本当做列表的每一行, 最后返回列表
     * @param str 源文本
     * @param encoding 编码
     * @param iLineExecuter ILineExecuter接口的实现
     * @return List&ltString&gt 文本列表 
     * @throws IOException
     */
    public static LinkedList<String> asLines(String str, String encoding, ILineExecutor iLineExecutor) throws IOException{
    	LinkedList<String> result = new LinkedList<String>();
    	if(str == null || str.isEmpty())
    		return result;
    	if(encoding == null || Key.EMPTY_STRING.equals(encoding.trim()))
    		encoding = DEFAULT_ENCODING;
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(str.getBytes(Charset.forName(encoding))), Charset.forName(encoding)));
    	String current;
    	while((current = bufferedReader.readLine())!=null){
    		if(!iLineExecutor.accept(current))
    			continue;
    		current = iLineExecutor.process(current);
    		if(current == null)
    			continue;
    		result.add(current);
    	}
    	bufferedReader.close();
    	return result;
    }
    
    /**
	 * 最长公共子序 列
	 * @param str1
	 * @param str2
	 * @param options 
	 * @return
	 */
	public static String longestCommonSubsequence(String str1, String str2, Map<String,String> options) {
		String[] temp = str1.split(Key.EMPTY_STRING);
		String[] str1AsArray = new String[temp.length+1];
		str1AsArray[0] = Key.EMPTY_STRING;
		for(int i=0; i<temp.length;i++)
			 str1AsArray[i+1] = temp[i];
		temp = str2.split(Key.EMPTY_STRING);
		String[] str2AsArray = new String[temp.length+1];
		str2AsArray[0] = Key.EMPTY_STRING;
		for(int i=0; i<temp.length; i++)
			str2AsArray[i+1] = temp[i];
		/*公共子序列的长度矩阵*/
		int[][] lcsValue = new int[str1AsArray.length][str2AsArray.length];
		/*由最优子结构性质计算出来的元素状态,标记两个串中的字符是否为一个公共字符*/
		String[][] lcsPath = new String[str1AsArray.length][str2AsArray.length];
		for(int i=0; i<lcsValue.length; i++)
			lcsValue[i][0] = 0;
		for(int i=0; i<lcsValue[0].length; i++)
			lcsValue[0][i] = 0;
		/*
		 * 定义 c[i, j] 表示长度为i的字符串x和长度为j的字符串y的公共子序列长度，有如下规则
		 * 
		 * 1.如果 i 或 j 是0， 说明x和y都是空串, c[i, j] = 0
		 * 2.如果 i>0,j>0 且两个串的末位相等，这是一个公共字符，两个串都去掉这个末尾, 剩下的部分又成为了一个的重叠子问题 c[i-1, j-1] = c[i, j] - 1
		 * 3.如果 i>0,j>0 且两个串的末位不相等, 那就得分别考虑一下，串x去掉最后一位和没去掉最后一位的串y比较(c[i-1, j])，还有串y去掉最后一位和没去掉最后的以为的串x(c[i, j-1])比较。
		 * 哪种情况获得的c[i, j]较大,就取哪种，大的说明去掉的是一个非公共字符，不影响问题。
		 */
		for(int i=1; i<lcsValue.length; i++) {
			for(int j=1; j<lcsValue[i].length; j++) {
				if(str1AsArray[i].equals(str2AsArray[j])) {
					lcsValue[i][j] = lcsValue[i-1][j-1] + 1;
					/*上边三条规则的结果，保存在lcsPath这个矩阵中,根据这个矩阵的值取结果*/
					lcsPath[i][j] = Key.UP_LEFT;
				}else if(lcsValue[i-1][j] >= lcsValue[i][j-1]) {
					lcsValue[i][j] = lcsValue[i-1][j];
					lcsPath[i][j] = Key.UP;
				}else {
					lcsValue[i][j] = lcsValue[i][j-1];
					lcsPath[i][j] = Key.LEFT;
				}
			}
		}
		/**
		 * 局部内部类 用于存储并返回公共子序列
		 * @author zhaoyang
		 */
		class LcsStructure{
			private List<String> lcsList = null;
			public LcsStructure() {
				lcsList = new LinkedList<String>();
			}
			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				for(int i=0; i<lcsList.size(); i++)
					sb.append(lcsList.get(i));
				return sb.toString();
			}
			/*
			 * 递归访问状态矩阵lcsPath， 取出其中 UP_LEFT 状态的， 说明这是个公共字符。<br>
			 * 至于为什么实现了一个局部内部类，就是因为这个方法是递归的, 没法在方法里给出一个变量用于保存符合要求的字符。
			 * 这个局部类, 用成员变量保存符合要求的字符, 然后把成员变量返回给外部方法 
			 */
			public void output(String[][] lcsPath, String[] str, int i, int j) {
				if(i == 0 || j == 0)
					return;
				if(lcsPath[i][j].equals(Key.UP_LEFT)) {
					output(lcsPath, str, i-1, j-1);
					lcsList.add(str[i]);
				}else if(lcsPath[i][j].equals(Key.UP))
					output(lcsPath, str, i-1, j);
				else
					output(lcsPath, str, i, j-1);
			}
		}
		LcsStructure lcsStructure = new LcsStructure();
		lcsStructure.output(lcsPath, str1AsArray, str1AsArray.length-1, str2AsArray.length-1);
		return lcsStructure.toString();
	}
	
	/* ANSII的编码最大为256 */
	private final static int TOP_OF_ANSII = 256;
	
	/**
	 * KMP算法
	 * @param text 文本串
 	 * @param pattern 模式串
	 * @return 若匹配成功, 返回匹配的首位置; 失败, 返回-1
	 */
	public static int kmp(String text, String pattern){
		/*  确定有限状态机(Deterministic Finite Automation)
		 *  表示的含义为 dfa[当前状态][将要遇到的字符] = 下个状态 
		 *  当前状态一共有(模式串长度+1)种,但是下标由0开始计
		 *  因此pattern.length即可表示全部状态长度
		 *  
		 *  例如pattern为abac, 则有 0 ~ 4 共计5个状态.
		 *  遍历完文本串以后, 只要状态推进到4, 就算匹配完成
		 *     a     b     a     c
		 *  0 --- 1 --- 2 --- 3 --- 4 */
		int[][] dfa = new int[pattern.length()][TOP_OF_ANSII];
		/* 0号状态遇到了模式串的第一个字符一定要进入1号状态 */
		dfa[0][pattern.charAt(0)] = 1;
		/* prevState 这个变量记录当前状态的前一个状态。
		 * 其状态跟随当前状态推进; 当当前状态重启(下标回溯)时, 先退回到 prevState*/
		int prevState = 0;
		/* 构造确定有限状态机 */
		for(int j=1, length=pattern.length(); j<length; j++){
			for(int character = 0; character < TOP_OF_ANSII; character++){
				dfa[j][character] = dfa[prevState][character];
			}
			dfa[j][pattern.charAt(j)] = j + 1;
			prevState = dfa[prevState][pattern.charAt(j)];
		}
		/* 开始搜索, 变量j为 即将到达的状态*/
		int j = 0;
		for(int i=0,length=text.length(); i<length; i++){
			/* 在文本串中遍历将要匹配的字符, 决定状态推进(j变大), 或状态重启(j变小)*/
			j = dfa[j][text.charAt(i)];
			if(j == pattern.length())
				return i - pattern.length() + 1;
		}
		return -1;
	}
	
	public static int stringToInt(String str) {
		byte[] bytestr = str.getBytes(Charset.forName("UTF-8"));
		return MurmurHash.hash32(ByteBuffer.wrap(bytestr), 0);
	}
}
