package com.buildnow.springbootapp.buildnowspringboot.controller;

import com.buildnow.springbootapp.buildnowspringboot.dto.applicationEvaluation.ApplicationEvaluationDTO;
import com.buildnow.springbootapp.buildnowspringboot.dto.applicationEvaluation.ApplicationEvaluationListDTO;
import com.buildnow.springbootapp.buildnowspringboot.dto.applicationEvaluation.ScoreResponseDTO;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.application.ApplicationEvaluation;
import com.buildnow.springbootapp.buildnowspringboot.service.ApplicationEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/application-evaluation")
public class ApplicationEvaluationController {
    private final ApplicationEvaluationService applicationEvaluationService;
    @PostMapping("/admin/{recruitmentId}/{applicationId}")
    public ResponseEntity<List<ApplicationEvaluation>> insertNewApplicationEvaluation(@PathVariable("recruitmentId") Long recruitmentId,
                                                               @PathVariable("applicationId") Long applicationId,
                                                               ApplicationEvaluationListDTO applicationEvaluationListDTO){
        List<ApplicationEvaluation> applicationEvaluationList = new ArrayList<>();
        for(ApplicationEvaluationDTO applicationEvaluationDTO : applicationEvaluationListDTO.getApplicationEvaluationDTOList()){
            ApplicationEvaluation applicationEvaluation = applicationEvaluationService.createNewApplicationEvaluation(recruitmentId,
                    applicationId,
                    applicationEvaluationDTO.getCategoryName(),
                    applicationEvaluationDTO.getScore());
            applicationEvaluationList.add(applicationEvaluation);
        }

        return new ResponseEntity<>(applicationEvaluationList, HttpStatus.CREATED);
    }

    @GetMapping("/recruiter/{recruitmentId}/{applicationId}")
    public ResponseEntity<List<ScoreResponseDTO>> retrieveScores(@PathVariable("recruitmentId") Long recruitmentId,
                                                                 @PathVariable("applicationId") Long applicationId,
                                                                 Authentication authentication
                                                                 ){
        List<ScoreResponseDTO> res = applicationEvaluationService.retrieveScores(recruitmentId, applicationId, authentication.getName());
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeExceptionHandler(RuntimeException ex){
        return new ResponseEntity<>("Error Occurred: " + ex.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
