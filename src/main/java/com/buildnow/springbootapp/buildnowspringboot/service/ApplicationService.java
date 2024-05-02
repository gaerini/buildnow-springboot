package com.buildnow.springbootapp.buildnowspringboot.service;

import com.buildnow.springbootapp.buildnowspringboot.dto.ApplicationDTO;
import com.buildnow.springbootapp.buildnowspringboot.dto.applicationEvaluation.ScoreResponseListDTO;
import com.buildnow.springbootapp.buildnowspringboot.dto.applier.ApplierWithScoreDTO;
import com.buildnow.springbootapp.buildnowspringboot.dto.applier.ApplierWithScoreListDTO;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.Applier;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.Recruiter;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.application.Application;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.application.TempSaved;
import com.buildnow.springbootapp.buildnowspringboot.entitiy.recruitment.Recruitment;
import com.buildnow.springbootapp.buildnowspringboot.repository.*;
import com.buildnow.springbootapp.buildnowspringboot.repository.tempSave.TempSavedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final ApplierRepository applierRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final HandedOutRepository handedOutRepository;
    private final RecruiterRepository recruiterRepository;
    private final GradingRepository gradingRepository;
    private final ApplicationEvaluationService applicationEvaluationService;
    private final Environment environment;

    @Transactional
    public Application createApplication(String applierName, Long recruitmentId) throws RuntimeException {
        Applier applier = applierRepository.findByUsername(applierName);
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new RuntimeException("해당하는 리크루트먼트가 없습니다."));
        if(applicationRepository.existsByApplierAndRecruitment(applier, recruitment)){
            Application existingApplication = findExistingApplication(applierName, recruitmentId);
            throw new RuntimeException("이미 지원한 내역 (applicationId: " + existingApplication.getId() + ") 이 있기 때문에 새로운 객체를 생성할 수 없습니다.");
        }
        Application newApplication = Application.builder()
                .applier(applier)
                .recruitment(recruitment)
                .build();
        applier.addApplication(newApplication);
        recruitment.addApplication(newApplication);
        //시간, 회사명, admin 링크
        SlackApi api = new SlackApi(Objects.requireNonNull(environment.getProperty("slack.webhook.token")));
        String message = "어플리케이션 생성 일시: " + LocalDateTime.now() + "\n" + "지원업체명: " + applier.getCompanyName() + "\n" + "admin 링크: https://buildnow-v1.vercel.app/bn_admin_sinhan/login";
        api.call(new SlackMessage(message));
        return applicationRepository.save(newApplication);
    }

    @Transactional
    public void deleteApplication(Long applicationId, String applierName) throws AuthenticationException {
        Applier applier = applierRepository.findByUsername(applierName);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));

        if(!application.getApplier().getUsername().equals(applierName)){
            throw new RuntimeException("삭제 권한이 없습니다.");
        }
        applicationRepository.delete(application);
    }

    @Transactional
    public List<Application> retrieveApplication( String applierName) {
        Applier applier = applierRepository.findByUsername(applierName);
        return applicationRepository.findByApplier(applier);
    }

    @Transactional
    public List<ApplicationDTO> retrieveAllApplication(){
       List<Application> applicationList =  applicationRepository.findAll();
       List<ApplicationDTO> applicationDTOList = new ArrayList<>();
       for(Application application : applicationList){
           ApplicationDTO applicationDTO = new ApplicationDTO(
                   application,
                   application.getApplier()
           );
           applicationDTOList.add(applicationDTO);
       }
       return applicationDTOList;
    }

    @Transactional
    public Application findExistingApplication(String applierName, Long recruitmentId){
        Applier applier = applierRepository.findByUsername(applierName);
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(()->new RuntimeException("해당하는 recruitment가 존재하지 않습니다."));
        return applicationRepository.findByApplierAndRecruitment(applier, recruitment);
    }

    @Transactional
    public void updateIsSubmitTrue(Long applicationId, String applierName){
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));
        Applier applier = applierRepository.findByUsername(applierName);

        if(!application.getApplier().getUsername().equals(applierName)){
            throw new RuntimeException("제출 권한이 없습니다.");
        }

        application.updateSubmitTrue();
    }

    @Transactional
    public void updateIsAdminCheckedTrue(Long applicationId){
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));
        application.updateIsAdminTrue();
    }

    @Transactional
    public void updateIsAdminCheckedFalse(Long applicationId){
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));
        application.updateIsAdminFalse();
    }

    @Transactional
    public void updateIsReadTrue(Long applicationId, String recruiterName){
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));
        if(!application.getRecruitment().getRecruiter().getUsername().equals(recruiterName)){
            throw new RuntimeException("해당 어플리케이션 상태를 업데이트할 권한이 없습니다.");
        }
        application.updateIsReadTrue();
    }

    @Transactional
    public void updateIsCheckedTrue(Long applicationId, String recruiterName){
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));
        if(!application.getRecruitment().getRecruiter().getUsername().equals(recruiterName)){
            throw new RuntimeException("해당 어플리케이션 상태를 업데이트할 권한이 없습니다.");
        }
        application.updateIsCheckedTrue();
    }

    @Transactional
    public void updateIsCheckedFalse(Long applicationId, String recruiterName){
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(()->new RuntimeException("애플리케이션이 존재하지 않습니다."));
        if(!application.getRecruitment().getRecruiter().getUsername().equals(recruiterName)){
            throw new RuntimeException("해당 어플리케이션 상태를 업데이트할 권한이 없습니다.");
        }
        application.updateIsCheckedFalse();
    }

    @Transactional
    public ApplierWithScoreListDTO retrieveApplicationByRecruitment(String recruiterName, Long recruitmentId){
        Recruiter recruiter = recruiterRepository.findByUsername(recruiterName);
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(()->new RuntimeException("해당하는 recruitment가 없습니다."));
        if(!recruitment.getRecruiter().equals(recruiter)){
            throw new RuntimeException("해당 recruitment의 회사가 아니기 때문에 권한 없음.");
        }

        List<Application> applicationList = applicationRepository.findByRecruitment(recruitment);
        ApplierWithScoreListDTO applierWithScoreListDTO = new ApplierWithScoreListDTO();
        applierWithScoreListDTO.setApplierWithScoreDTOList(new ArrayList<>());
        for(Application application : applicationList){
            ApplierWithScoreDTO applierWithScoreDTO = new ApplierWithScoreDTO();
            TempSaved tempSaved = application.getTempSaved();
            List<ScoreResponseListDTO> temp = applicationEvaluationService.retrieveScores(recruitmentId, application.getId(), recruiterName);
            applierWithScoreDTO.setChecked(application.isChecked());
            applierWithScoreDTO.setRead(application.isRead());
            applierWithScoreDTO.setAdminChecked(application.isAdminChecked());
            applierWithScoreDTO.setTempPrerequisiteList(application.getTempPrerequisiteList());
            applierWithScoreDTO.setWorkType(application.getWorkTypeApplying());
            applierWithScoreDTO.setCompanyName(application.getApplier().getCompanyName());
            applierWithScoreDTO.setLicenseName(tempSaved.getLicenseName());
            applierWithScoreDTO.setScoreList(temp);
            applierWithScoreDTO.setApplicationId(application.getId());
            applierWithScoreListDTO.getApplierWithScoreDTOList().add(applierWithScoreDTO);
        }
        return applierWithScoreListDTO;
    }
}
