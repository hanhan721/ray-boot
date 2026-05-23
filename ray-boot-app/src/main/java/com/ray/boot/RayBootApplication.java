package com.ray.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.Inet4Address;
import java.net.NetworkInterface;

@SpringBootApplication(scanBasePackages = "com.ray")
@MapperScan("com.ray.**.mapper")
@EnableAsync
@EnableScheduling
public class RayBootApplication {

    private final Environment env;

    RayBootApplication(Environment env) {
        this.env = env;
    }

    static void main(String[] args) {
        SpringApplication.run(RayBootApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void onStarted() {
        var port = env.getProperty("local.server.port", "8080");
        var contextPath = env.getProperty("server.servlet.context-path", "");

        var banner = """

                          _oo0oo_
                         o8888888o
                         88" . "88
                         (| -_- |)
                         0\\  =  /0
                       ___/`---'\\___
                     .' \\\\|     |// '.
                    / \\\\|||  :  |||// \\
                   / _||||| -:- |||||- \\
                  |   | \\\\\\  -  /// |   |
                  | \\_|  ''\\---/''  |_/ |
                  \\  .-\\__  '-'  ___/-. /
                ___'. .'  /--.--\\  `. .'___
             ."" '<  `.___\\_<|>_/___.' >' "".
            | | :  `- \\`.;`\\ _ /`;.`/ - ` : | |
            \\  \\ `_.   \\_ __\\ /__ _/   .-` /  /
        =====`-.____`.___ \\_____/___.-`___.-'=====
                          `=---='

             佛祖保佑       永无BUG       永不宕机

        """;

        var sb = new StringBuilder(banner);
        sb.append("\n");
        sb.append("  =========================================================\n");
        sb.append("  :: ray-boot 启动成功 ::                                 \n");
        sb.append("  =========================================================\n");
        sb.append("  接口文档:\n");
        sb.append("    - Local:   http://localhost:%s%s/scalar\n".formatted(port, contextPath));

        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var addr = addresses.nextElement();
                    if (addr instanceof Inet4Address ipv4) {
                        sb.append("    - Network: http://%s:%s%s/scalar  (%s)\n"
                                .formatted(ipv4.getHostAddress(), port, contextPath, ni.getDisplayName()));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        sb.append("  =========================================================\n");

        System.out.println(sb);
    }
}
