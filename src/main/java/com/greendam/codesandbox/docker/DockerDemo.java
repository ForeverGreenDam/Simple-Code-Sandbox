package com.greendam.codesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * Java操作Docker实例
 * @author ForeverGreenDam
 */
public class DockerDemo {
    public static void main(String[] args) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        dockerClient.versionCmd().exec();
        System.out.println("ok");
    }
}
