package com.kbait.anchack.rental.resolver;

import com.kbait.anchack.rental.dto.external.RawRentalTransaction;

public interface RentalAdminDongResolver {

    ResolutionSession openSession();

    interface ResolutionSession extends AutoCloseable {

        RentalAdminDongResolution resolve(RawRentalTransaction rawTransaction);

        @Override
        void close();
    }
}
