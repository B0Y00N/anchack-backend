package com.kbait.anchack.place.cctv.service;

import com.kbait.anchack.place.cctv.converter.PublicCctvPlaceConverter;
import com.kbait.anchack.place.cctv.dto.PublicCctvRow;
import com.kbait.anchack.place.cctv.parser.PublicCctvCsvParser;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import lombok.RequiredArgsConstructor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CctvPlaceCollectionServiceImpl implements CctvPlaceCollectionService {

    private final PublicCctvCsvParser csvParser;
    private final PublicCctvPlaceConverter placeConverter;

    @Override
    public List<ExternalPlace> collect(Reader reader) {
        List<PublicCctvRow> rows = csvParser.parse(reader);
        List<ExternalPlace> places = new ArrayList<>();

        for (PublicCctvRow row : rows) {
            places.add(placeConverter.convert(row));
        }

        return List.copyOf(places);
    }
}
