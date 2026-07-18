package org.example.sincronizador2.services;

import java.io.InputStream;

public interface MinioService {

    void uploadFile(String objectName, InputStream stream, long size, String contentType);

}

