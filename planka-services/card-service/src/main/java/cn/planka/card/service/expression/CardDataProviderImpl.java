package cn.planka.card.service.expression;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.Yield;
import cn.planka.card.repository.CardRepository;
import cn.planka.domain.card.CardId;
import cn.planka.infra.expression.CardDataProvider;
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
