CREATE TABLE public.number (
    id BIGINT CONSTRAINT number__pk__id PRIMARY KEY,
    value BIGINT CONSTRAINT number__not_null__value NOT NULL
);

CREATE SEQUENCE number__seq__id INCREMENT BY 1 OWNED BY public.number.id;
