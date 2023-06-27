package baidu.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class KafkaProducerDemo {
    public static void main(String[] args) throws IOException {
        // 需要自行配置下面三个参数
        // kafka.properties所在路径（建议写文件所在的绝对路径）
        String path = "kafka.properties";
        // 主题名称-topic name
        String topic = "test";
        // 消息内容
        String message = "kafka java test";

        // 创建配置类，并获取配置文件 kafka.properties 的内容。
        Properties properties = new Properties();
        File file = new File(path);
        try {
            if (file.exists()) {
                // 如果通过用户指定的path路径找到了kafka.properties文件，则加载kafka.properties中的配置项
                properties.load(new FileInputStream(file));
            } else {
                // 如果没有从path中找到，则从KafkaProducerDemo所在的路径去查找
                properties.load(
                        KafkaProducerDemo.class.getClassLoader().getResourceAsStream("kafka.properties")
                );
            }
        } catch (IOException e) {
            // 没找到kafka.properties文件，在此处处理异常
            throw e;
        }

        // 设置 java.security.auth.login.config，用于加载kafka_client_jaas.conf文件
        if (null == System.getProperty("java.security.auth.login.config")) {
            System.setProperty(
                    "java.security.auth.login.config",
                    properties.getProperty("java.security.auth.login.config")
            );
        }

        // Kafka消息的序列化方式。
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        // 请求的最长等待时间。
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30 * 1000);
        // 设置客户端内部重试次数。
        properties.put(ProducerConfig.RETRIES_CONFIG, 5);
        // 设置客户端内部重试间隔。
        properties.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 3000);

        // 构建kafkaProducer对象
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        try {
            // 向指定的topic发送100条消息
            for (int i = 0; i < 100; i++) {
                // 通过 ProducerRecord 构造一个消息对象
                ProducerRecord<String, String> kafkaMessage =  new ProducerRecord<>(topic, message + "-" + i);
                // 通过kafkaProducer发送消息
                kafkaProducer.send(kafkaMessage, (RecordMetadata recordMetadata, Exception e) -> {
                    // 发送信息后的回调函数，用以验证消息是否发送成功
                    if (e == null) {
                        System.out.println("send success:" + recordMetadata.toString());
                    } else {
                        e.printStackTrace();
                        System.err.println("send failed");
                    }
                });
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }finally {
            // 不要忘记关闭资源
            kafkaProducer.close();
        }
    }
}
