package org.devgateway.toolkit.forms.wicket.components.form;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.editor.SummernoteStorage;
import org.apache.commons.io.IOUtils;
import org.devgateway.toolkit.persistence.dao.FileContent;
import org.devgateway.toolkit.persistence.dao.FileMetadata;
import org.devgateway.toolkit.persistence.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class SummernoteJpaStorageService implements SummernoteStorage {

    public static final String STORAGE_ID = "jpaStorage";

    @Autowired
    private FileMetadataRepository repository;


    @Override
    public void writeContent(final String imageName, final InputStream inputStream) {
        byte[] bytes = null;
        try {
            bytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setName(imageName);
        fileMetadata.setSize(bytes.length);
        FileContent fileContent = new FileContent();
        fileContent.setBytes(bytes);
        fileMetadata.setContent(fileContent);
        repository.save(fileMetadata);

    }

    @Override
    public byte[] getContent(final String imageName) {
        FileMetadata byKey = repository.findByName(imageName);
        if (byKey == null || byKey.getContent() == null) {
            return null;
        }
        return byKey.getContent().getBytes();
    }

    @Override
    public String getId() {
        return STORAGE_ID;
    }
}
