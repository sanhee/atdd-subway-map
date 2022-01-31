package nextstep.subway.applicaion;

import nextstep.subway.applicaion.dto.SectionRequest;
import nextstep.subway.applicaion.exception.NewDownStationDuplicateException;
import nextstep.subway.applicaion.exception.NotRegisterDownStationException;
import nextstep.subway.applicaion.exception.NotRemoveStationException;
import nextstep.subway.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectionService {
    private final SectionRepository sectionRepository;
    private final LineRepository lineRepository;
    private final StationRepository stationRepository;

    public SectionService(SectionRepository sectionRepository, LineRepository lineRepository, StationRepository stationRepository) {
        this.sectionRepository = sectionRepository;
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
    }

    public Section saveSection(Long lineId, SectionRequest sectionRequest) {
        Line findLine = lineRepository.findById(lineId)
                                      .orElseThrow(() -> new RuntimeException("해당하는 노선을 찾을 수 없습니다."));
        Long newUpStationId = Long.valueOf(sectionRequest.getUpStationId());
        Long newDownStationId = Long.valueOf(sectionRequest.getDownStationId());

        boolean isExistDown = false;
        for (Section section : findLine.getSectionList()) {
            Long findDownStationId = section.getDownStation()
                                            .getId();
            Long findUpStationId = section.getUpStation()
                                          .getId();

            if (newUpStationId.equals(findDownStationId)) {
                isExistDown = true;
            }

            if (newDownStationId.equals(findDownStationId) || newDownStationId.equals(findUpStationId)) {
                throw new NewDownStationDuplicateException();
            }
        }

        if (!isExistDown) {
            throw new NotRegisterDownStationException();
        }


        Station upStation = stationRepository.findById(newUpStationId)
                                             .orElseThrow(() -> new RuntimeException("존재하지 않는 상행역 입니다."));
        Station downStation = stationRepository.findById(newDownStationId)
                                               .orElseThrow(() -> new RuntimeException("존재하지 않는 하행역 입니다."));

        Section saveSection = new Section(findLine, upStation, downStation);

        return sectionRepository.save(saveSection);
    }

    @Transactional
    public void delete(Long lineId, Long deleteStationId) {
        Line findLine = lineRepository.findById(lineId)
                                      .orElseThrow(() -> new RuntimeException("해당하는 노선을 찾을 수 없습니다."));


        if(findLine.getSectionList().size() == 1){
            throw new NotRemoveStationException("구간이 1개인 경우 역을 삭제할 수 없다.");
        }

        boolean isExistStation = false;
        for (Section section : findLine.getSectionList()) {
            Long findDownStationId = section.getDownStation()
                                            .getId();
            Long findUpStationId = section.getUpStation()
                                          .getId();

            if (deleteStationId.equals(findUpStationId)) {
                throw new NotRemoveStationException("지하철 노선에 등록된 하행 종점역만 제거할 수 있다.");
            }

            if (deleteStationId.equals(findDownStationId)) {
                isExistStation = true;
            }
        }

        if (!isExistStation) {
            throw new NotRemoveStationException("지하철 노선에 등록된 역만 제거할 수 있다.");
        }


        sectionRepository.deleteByLineIdAndDownStationId(lineId, deleteStationId);
    }
}
