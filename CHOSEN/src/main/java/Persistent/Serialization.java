package Persistent;

/**
 * ObjectMapper是JSON操作的核心，Jackson的所有JSON操作都是在ObjectMapper中实现。
 * ObjectMapper有多个JSON序列化的方法，可以把JSON字符串保存File、OutputStream等不同的介质中。
 * writeValue(File arg0, Object arg1)把arg1转成json序列，并保存到arg0文件中。
 * writeValue(OutputStream arg0, Object arg1)把arg1转成json序列，并保存到arg0输出流中。
 * writeValueAsBytes(Object arg0)把arg0转成json序列，并把结果输出成字节数组。
 * writeValueAsString(Object arg0)把arg0转成json序列，并把结果输出成字符串。
 */
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 将对象序列化
 */
public class Serialization {

    private static ObjectMapper mapper =new ObjectMapper();
    /**
     * 将对象转化为JSON文件
     * @return
     */
    public static String ObjToJSON(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    /**
     * 将JSON文件转为Obj对象
     * @return
     */
    public static <T> T json2BeanByType(String jsonStr, TypeReference tr) throws JsonProcessingException {
        return (T) mapper.readValue(jsonStr,tr);
    }
    public static <T> T json2Bean(String jsonStr,Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(jsonStr,clazz);
    }
}
