package com.pms.property.draft.repository;

import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.entity.DraftStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDraftRepository extends JpaRepository<PropertyDraftEntity, Long> {

	List<PropertyDraftEntity> findByStatusInOrderByUpdatedAtDesc(Collection<DraftStatus> statuses);

			Optional<PropertyDraftEntity> findFirstByPublishedPropertyIdAndStatusOrderByUpdatedAtDesc(
				String publishedPropertyId,
				DraftStatus status
			);

			List<PropertyDraftEntity> findByPublishedPropertyIdInAndStatus(Collection<String> publishedPropertyIds, DraftStatus status);

	long deleteByPublishedPropertyId(String publishedPropertyId);
}

