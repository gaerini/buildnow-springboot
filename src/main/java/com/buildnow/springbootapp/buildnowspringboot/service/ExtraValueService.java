package com.buildnow.springbootapp.buildnowspringboot.service;

import com.buildnow.springbootapp.buildnowspringboot.dto.extraValue.ExtraValueDTO;
import com.buildnow.springbootapp.buildnowspringboot.dto.extraValue.ExtraValueListDTO;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.Applier;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.applierInfo.ExtraValue;
import com.buildnow.springbootapp.buildnowspringboot.repository.ApplierRepository;
import com.buildnow.springbootapp.buildnowspringboot.repository.ExtraValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtraValueService {
    private final ExtraValueRepository extraValueRepository;
    private final ApplierRepository applierRepository;
    @Transactional
    public List<ExtraValue> createExtraValue(Long applierId, ExtraValueListDTO extraValueListDTO){
        Applier applier = applierRepository.findById(applierId)
                .orElseThrow(()->new RuntimeException("해당하는 applier가 없습니다."));
        List<ExtraValue> res = new ArrayList<>();
        for(ExtraValueDTO value : extraValueListDTO.getExtraValueList()){
            if(extraValueRepository.existsByCategoryAndApplier(value.getCategory(), applier)){
                throw new RuntimeException("이미 해당 applier에 동일한 category가 저장되어있습니다.");
            }
            ExtraValue newExtraValue = ExtraValue.builder()
                    .category(value.getCategory())
                    .value(value.getValue())
                    .build();
            applier.addExtraValue(newExtraValue);
            extraValueRepository.save(newExtraValue);
            res.add(newExtraValue);
        }
        return res;
    }

    @Transactional
    public List<ExtraValue> retrieveExtraValues(Long applierId){
        Applier applier = applierRepository.findById(applierId)
                .orElseThrow(()->new RuntimeException("해당하는 applier가 없습니다."));
        return applier.getExtraValueList();
    }

    @Transactional
    public ExtraValue updateExtraValue(Long applierId, ExtraValueDTO extraValueDTO){
        Applier applier = applierRepository.findById(applierId)
                .orElseThrow(()->new RuntimeException("해당하는 applier가 없습니다."));
        ExtraValue extraValue = extraValueRepository.findByCategoryAndApplier(extraValueDTO.getCategory(), applier);
        if(extraValue == null){
            throw new RuntimeException("해당하는 category와 일치하는 extra value가 없습니다.");
        }
        extraValue.updateValue(extraValueDTO.getValue());
        return extraValueRepository.save(extraValue);
    }

    @Transactional
    public void removeExtraValue(Long applierId, String category){
        Applier applier = applierRepository.findById(applierId)
                .orElseThrow(()->new RuntimeException("해당하는 applier가 없습니다."));
        ExtraValue extraValue = extraValueRepository.findByCategoryAndApplier(category, applier);
        if(extraValue == null){
            throw new RuntimeException("해당하는 category와 일치하는 extra value가 없습니다.");
        }
        applier.removeExtraValue(extraValue);
        extraValueRepository.delete(extraValue);
    }
}