package org.lattelang.idea;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * latte file type factory
 */
public class LatteFileTypeFactory extends FileTypeFactory {
        @Override
        public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
                fileTypeConsumer.consume(LatteFileType.INSTANCE);
        }
}
