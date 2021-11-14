package win.liumian.qt.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author liumian
 * @date 2021/11/6 12:27 下午
 */
@Slf4j
public class BannerUtil {

    public static void printGitBuildInfo() {
        try {
            Resource resource = new ClassPathResource("gitBuildInfo.properties");
            InputStream is = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("=")) {
                    String[] split = line.split("=");
                    if (split.length == 2) {
                        System.setProperty(split[0], split[1]);
                    }
                }
                log.info(line);
            }
        } catch (Exception e) {
            log.error("打印gitBuildInfo异常", e);
        }
    }

}
