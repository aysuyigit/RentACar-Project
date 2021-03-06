package com.etiya.rentACar.business.concretes;

import com.etiya.rentACar.business.abstracts.MessageKeyService;
import com.etiya.rentACar.business.abstracts.MessageService;
import com.etiya.rentACar.business.constants.messages.Messages;
import com.etiya.rentACar.business.request.messageKeyRequests.CreateMessageKeyRequest;
import com.etiya.rentACar.business.request.messageKeyRequests.DeleteMessageKeyRequest;
import com.etiya.rentACar.business.request.messageKeyRequests.UpdateMessageKeyRequest;
import com.etiya.rentACar.core.utilities.mapping.ModelMapperService;
import com.etiya.rentACar.core.utilities.results.Result;
import com.etiya.rentACar.core.utilities.results.SuccessResult;
import com.etiya.rentACar.dataAccess.abstracts.MessageKeyDao;
import com.etiya.rentACar.entities.multipleLanguageMessages.MessageKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageKeyManager implements MessageKeyService {

    private MessageKeyDao messageKeyDao;
    private ModelMapperService modelMapperService;
    private MessageService messageService;

    @Autowired
    public MessageKeyManager(MessageKeyDao messageKeyDao, ModelMapperService modelMapperService,MessageService messageService) {
        this.messageKeyDao = messageKeyDao;
        this.modelMapperService = modelMapperService;
        this.messageService=messageService;
    }

    @Override
    public Result save(CreateMessageKeyRequest createMessageKeyRequest) {
        MessageKey messageKey = modelMapperService.forRequest().map(createMessageKeyRequest, MessageKey.class);
        this.messageKeyDao.save(messageKey);
        return new SuccessResult(messageService.getMessage(Messages.addMessageKey));
    }

    @Override
    public Result delete(DeleteMessageKeyRequest deleteMessageKeyRequest) {
        MessageKey messageKey = modelMapperService.forRequest().map(deleteMessageKeyRequest, MessageKey.class);
        this.messageKeyDao.delete(messageKey);
        return new SuccessResult(messageService.getMessage(Messages.deleteMessageKey));
    }

    @Override
    public Result update(UpdateMessageKeyRequest updateMessageKeyRequest) {
        MessageKey messageKey = modelMapperService.forRequest().map(updateMessageKeyRequest, MessageKey.class);
        this.messageKeyDao.save(messageKey);
        return new SuccessResult(messageService.getMessage(Messages.updateMessageKey));
    }
}
