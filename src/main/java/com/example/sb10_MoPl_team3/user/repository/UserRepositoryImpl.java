package com.example.sb10_MoPl_team3.user.repository;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.dto.request.UserSearchCondition;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<User> searchUsers(UserSearchCondition condition, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);

        String sortBy = condition.sortBy();
        boolean ascending = "ASCENDING".equals(condition.sortDirection());

        List<Predicate> predicates = buildFilterPredicates(cb, root, condition);

        Predicate cursorPredicate = buildCursorPredicate(cb, root, condition, sortBy, ascending);
        if (cursorPredicate != null) {
            predicates.add(cursorPredicate);
        }

        Expression<?> sortExpression = getSortExpression(cb, root, sortBy);

        query.select(root)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(
                        ascending ? cb.asc(sortExpression) : cb.desc(sortExpression),
                        ascending ? cb.asc(root.get("id")) : cb.desc(root.get("id"))
                );

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public long countUsers(UserSearchCondition condition) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<User> root = query.from(User.class);

        List<Predicate> predicates = buildFilterPredicates(cb, root, condition);

        query.select(cb.count(root))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    private List<Predicate> buildFilterPredicates(
            CriteriaBuilder cb,
            Root<User> root,
            UserSearchCondition condition
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (condition.emailLike() != null && !condition.emailLike().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(root.get("email")),
                    "%" + condition.emailLike().toLowerCase(Locale.ROOT) + "%"
            ));
        }

        if (condition.roleEqual() != null) {
            predicates.add(cb.equal(root.get("role"), condition.roleEqual()));
        }

        if (condition.isLocked() != null) {
            if (condition.isLocked()) {
                predicates.add(cb.equal(root.get("status"), UserStatus.LOCKED));
            } else {
                predicates.add(cb.notEqual(root.get("status"), UserStatus.LOCKED));
            }
        }

        return predicates;
    }

    private Predicate buildCursorPredicate(
            CriteriaBuilder cb,
            Root<User> root,
            UserSearchCondition condition,
            String sortBy,
            boolean ascending
    ) {
        boolean hasCursor = condition.cursor() != null && !condition.cursor().isBlank();
        boolean hasIdAfter = condition.idAfter() != null;

        if (!hasCursor && !hasIdAfter) {
            return null;
        }

        if (hasCursor != hasIdAfter) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }

        Expression<?> sortExpression = getSortExpression(cb, root, sortBy);
        Comparable<?> cursorValue = parseCursorValue(sortBy, condition.cursor());

        return compareCursor(cb, root, sortExpression, cursorValue, condition.idAfter(), ascending);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate compareCursor(
            CriteriaBuilder cb,
            Root<User> root,
            Expression<?> sortExpression,
            Comparable<?> cursorValue,
            UUID idAfter,
            boolean ascending
    ) {
        Expression expression = sortExpression;
        Comparable value = cursorValue;
        Path<UUID> idPath = root.get("id");

        Predicate valueCondition = ascending
                ? cb.greaterThan(expression, value)
                : cb.lessThan(expression, value);

        Predicate idCondition = ascending
                ? cb.greaterThan(idPath, idAfter)
                : cb.lessThan(idPath, idAfter);

        return cb.or(
                valueCondition,
                cb.and(
                        cb.equal(sortExpression, cursorValue),
                        idCondition
                )
        );
    }

    private Expression<?> getSortExpression(CriteriaBuilder cb, Root<User> root, String sortBy) {
        return switch (sortBy) {
            case "name" -> root.get("name");
            case "email" -> root.get("email");
            case "createdAt" -> root.get("createdAt");
            case "role" -> root.get("role");
            case "isLocked" -> cb.<Integer>selectCase()
                    .when(cb.equal(root.get("status"), UserStatus.LOCKED), 1)
                    .otherwise(0);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }

    private Comparable<?> parseCursorValue(String sortBy, String cursor) {
        try {
            return switch (sortBy) {
                case "name", "email" -> cursor;
                case "createdAt" -> Instant.parse(cursor);
                case "role" -> UserRole.valueOf(cursor);
                case "isLocked" -> parseLockedCursor(cursor);
                default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            };
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException) {
                throw exception;
            }
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    private Integer parseLockedCursor(String cursor) {
        if ("true".equalsIgnoreCase(cursor)) {
            return 1;
        }

        if ("false".equalsIgnoreCase(cursor)) {
            return 0;
        }

        throw new BusinessException(ErrorCode.INVALID_CURSOR);
    }

}
