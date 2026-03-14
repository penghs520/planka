package cn.agilean.kanban.card.service.expression;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.Yield;
import cn.agilean.kanban.card.repository.CardRepository;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.infra.expression.CardDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardDataProviderImpl implements CardDataProvider {

    private final CardRepository cardRepository;

    @Override
    public CardDTO findCardById(CardId cardId, Yield yield) {
        return cardRepository.findById(cardId, yield, "system").orElse(null);
    }
}
