package br.com.tecsus.sigaubs.repositories;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.mockito.Answers;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RepositoryMockSupport {

    private RepositoryMockSupport() {
    }

    @SuppressWarnings("unchecked")
    static <T> TypedQuery<T> typedQuery(List<T> results) {
        TypedQuery<T> query = mock(TypedQuery.class, Answers.RETURNS_SELF);
        when(query.getResultList()).thenReturn(results);
        return query;
    }

    @SuppressWarnings("unchecked")
    static <T> TypedQuery<T> typedQuery(List<T> results, T singleResult) {
        TypedQuery<T> query = mock(TypedQuery.class, Answers.RETURNS_SELF);
        when(query.getResultList()).thenReturn(results);
        when(query.getSingleResult()).thenReturn(singleResult);
        return query;
    }

    static Query nativeQuery(List<?> results) {
        Query query = mock(Query.class, Answers.RETURNS_SELF);
        when(query.getResultList()).thenReturn(results);
        return query;
    }

    static Query nativeSingleResult(Object result) {
        Query query = mock(Query.class, Answers.RETURNS_SELF);
        when(query.getSingleResult()).thenReturn(result);
        return query;
    }
}
