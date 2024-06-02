package plus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class UtilsPlus {
    /**
     * 实现多个bytes数组的相加
     * @param arrays
     * @return
     */
    public static byte[] concatenateByteArrays(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * 实现Gzip数据的解压
     * @param compressed
     * @return
     * @throws IOException
     */
    public static byte[] gzipDecompress(byte[] compressed) throws IOException {
        if (compressed == null || compressed.length == 0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(compressed));

        byte[] buffer = new byte[256];
        int n;
        while ((n = gunzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }

        // Close the streams
        gunzip.close();
        out.close();

        // Get the uncompressed data
        return out.toByteArray();
    }

    // 读取响应流并编码
    public static String readBodyFromStream(InputStream inputStream, String encoding) throws Exception {
        // 如果编码为null，则使用UTF-8作为默认编码
        if (encoding == null) {
            encoding = String.valueOf(StandardCharsets.UTF_8);
        }

        try (Reader reader = new InputStreamReader(inputStream, encoding)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString();
            }
        }
    }

    //从 http conn 对象的头字典整理出原始响应头
    public static String getHeaderByHeaderFields(Map<String, List<String>> mapHeaders){
        // 获取响应头字段映射
        //Map<String, List<String>> mapHeaders = httpsConn.getHeaderFields();

        //存放首行
        String head_line = "";
        //存放其他行
        String other_line = "";

        for (Map.Entry<String, List<String>> entry : mapHeaders.entrySet()) {
            String key = entry.getKey();

            List<String> values = entry.getValue();
            StringBuilder value = new StringBuilder();
            for(String v:values){
                value.append(v);
            }

            if (key==null){
                head_line = String.format("%s\r\n", mapHeaders.get(null).get(0));
            } else {
                other_line += String.format("%s: %s\r\n", key, value);
                //BurpExtender.stdout.println(String.format("%s: %s\r\n", key, value));
            }
        }
        //BurpExtender.stdout.println(head_line + other_line);
        return head_line + other_line + "\r\n";
    }

    //从 http conn 对象信息中 获取响应体编码
    private static String getEncoding(String contentType){
        // 假设默认编码为UTF-8
        String encoding = StandardCharsets.UTF_8.name();

        // 尝试从内容类型中解析出字符编码
        if (contentType != null && contentType.contains("charset=")) {
            String charsetPart = contentType.split("charset=")[1];
            if (charsetPart != null && !charsetPart.trim().isEmpty()) {
                encoding = charsetPart.trim();
            }
        }

        return encoding;
    }

    // 读取响应流 使用系统编码
    public static String readBodyFromStream(InputStream inputStream) throws Exception {
        try (Reader reader = new InputStreamReader(inputStream)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString();
            }
        }
    }

    // 读取响应流 使用字节码
    public static byte[] readBytesFromStream(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static byte[] getBodyBytes(byte[] respBytes, int bodyOffset) {
        // 确保bodyOffset不会导致数组越界
        int maxLength = respBytes.length - bodyOffset;
        int bodyLength = Math.max(0, maxLength);

        // 从request数组中复制请求体的部分
        byte[] body = Arrays.copyOfRange(respBytes, bodyOffset, bodyOffset + bodyLength);
        return body;
    }
}
