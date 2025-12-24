--
-- PostgreSQL database dump
--

\restrict ilUCYfQDRZFOJRjHxOvnrG62hvfRFcv8sZG16UvooFH0Q8A7br0Er31yNwuJyOf

-- Dumped from database version 17.7 (bdc8956)
-- Dumped by pg_dump version 18.1

-- Started on 2025-12-22 10:01:15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 3 (class 3079 OID 16497)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 3476 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 2 (class 3079 OID 16486)
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- TOC entry 3477 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- TOC entry 271 (class 1255 OID 65920)
-- Name: update_device_last_seen(); Type: FUNCTION; Schema: public; Owner: neondb_owner
--

CREATE FUNCTION public.update_device_last_seen() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    UPDATE device_fingerprints
    SET last_seen = now()
    WHERE id = NEW.fingerprint_id;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_device_last_seen() OWNER TO neondb_owner;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 224 (class 1259 OID 65903)
-- Name: admin_logs; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.admin_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    admin_id uuid,
    action character varying(50) NOT NULL,
    target_type character varying(50) NOT NULL,
    target_id uuid,
    description text,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.admin_logs OWNER TO neondb_owner;

--
-- TOC entry 219 (class 1259 OID 65794)
-- Name: admins; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.admins (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(100),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    last_login_at timestamp without time zone,
    CONSTRAINT ck_email_format CHECK (((email)::text ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'::text))
);


ALTER TABLE public.admins OWNER TO neondb_owner;

--
-- TOC entry 220 (class 1259 OID 65808)
-- Name: device_fingerprints; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.device_fingerprints (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    fingerprint_hash character varying(255) NOT NULL,
    ip character varying(45) NOT NULL,
    user_agent text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    last_seen timestamp without time zone DEFAULT now() NOT NULL,
    is_blocked boolean DEFAULT false NOT NULL,
    block_reason text,
    blocked_by_admin_id uuid,
    blocked_at timestamp without time zone
);


ALTER TABLE public.device_fingerprints OWNER TO neondb_owner;

--
-- TOC entry 222 (class 1259 OID 65857)
-- Name: poll_options; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.poll_options (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    text character varying(500) NOT NULL,
    order_num integer DEFAULT 0 NOT NULL,
    CONSTRAINT ck_order_num CHECK ((order_num >= 0))
);


ALTER TABLE public.poll_options OWNER TO neondb_owner;

--
-- TOC entry 221 (class 1259 OID 65829)
-- Name: polls; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.polls (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(255) NOT NULL,
    question text NOT NULL,
    type character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    multiple_answers boolean DEFAULT false NOT NULL,
    show_results boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    closed_at timestamp without time zone,
    closed_by_admin_id uuid,
    organizer_fingerprint_id uuid NOT NULL,
    CONSTRAINT polls_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'CLOSED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT polls_type_check CHECK (((type)::text = ANY ((ARRAY['SINGLE'::character varying, 'MULTIPLE'::character varying, 'RATING'::character varying, 'OPEN'::character varying])::text[])))
);


ALTER TABLE public.polls OWNER TO neondb_owner;

--
-- TOC entry 223 (class 1259 OID 65873)
-- Name: votes; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    poll_id uuid NOT NULL,
    option_id uuid,
    fingerprint_id uuid NOT NULL,
    voted_at timestamp without time zone DEFAULT now() NOT NULL,
    text_answer text,
    CONSTRAINT ck_vote_data CHECK (((option_id IS NOT NULL) OR (text_answer IS NOT NULL)))
);


ALTER TABLE public.votes OWNER TO neondb_owner;

--
-- TOC entry 3470 (class 0 OID 65903)
-- Dependencies: 224
-- Data for Name: admin_logs; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.admin_logs (id, admin_id, action, target_type, target_id, description, created_at) FROM stdin;
046ffee4-0e10-456c-945b-c709f93cc46a	11111111-1111-1111-1111-111111111111	BLOCK_DEVICE	DeviceFingerprint	80d27290-1e1f-4bc4-8638-1b1042ec1312	Blocked device: Підозріли активність - багато голосів	2025-12-22 09:55:58.09111
72b250d7-6715-42e3-9324-cc8f8f50d101	11111111-1111-1111-1111-111111111111	UNBLOCK_DEVICE	DeviceFingerprint	80d27290-1e1f-4bc4-8638-1b1042ec1312	Unblocked device	2025-12-22 09:56:12.733035
\.


--
-- TOC entry 3465 (class 0 OID 65794)
-- Dependencies: 219
-- Data for Name: admins; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.admins (id, email, password_hash, name, is_active, created_at, last_login_at) FROM stdin;
11111111-1111-1111-1111-111111111111	admin@test.com	$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUmjL86GyNjHKGPF1i	Admin	t	2025-12-22 08:50:58.387299	2025-12-22 08:50:58.387299
\.


--
-- TOC entry 3466 (class 0 OID 65808)
-- Dependencies: 220
-- Data for Name: device_fingerprints; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.device_fingerprints (id, fingerprint_hash, ip, user_agent, created_at, last_seen, is_blocked, block_reason, blocked_by_admin_id, blocked_at) FROM stdin;
bb49bd02-24e8-4403-bc30-bde38261f8d5	d37a7c2fcca7a36163a45a40895f78c2404fc223d8d2c940c0bef0ae04a55fa2	string	string	2025-12-22 09:51:37.722482	2025-12-22 08:53:31.629751	f	\N	\N	\N
e0595da5-702b-4fef-b338-18de3ec97331	db355ffe0301db37843ac7eb7e59381edefd7ca7642a10f9e133ba652db7b831	string	string	2025-12-22 09:51:18.818056	2025-12-22 08:53:50.513796	f	\N	\N	\N
80d27290-1e1f-4bc4-8638-1b1042ec1312	e2db37c39255a1af069b68c05004909c3a8e9423c529a5277270d0e8fb272b11	string	string	2025-12-22 09:51:45.142522	2025-12-22 08:53:40.973949	f	\N	\N	\N
77e8acfc-0cea-4d21-b6c0-4cf50e635d4c	47c27715a054b6aeee60a3a7fa7bcd613398fae68af6881be2d3444352f0e5bd	string	string	2025-12-22 09:57:54.758689	2025-12-22 08:58:36.163872	f	\N	\N	\N
\.


--
-- TOC entry 3468 (class 0 OID 65857)
-- Dependencies: 222
-- Data for Name: poll_options; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.poll_options (id, poll_id, text, order_num) FROM stdin;
151d6eaf-25cf-4d7e-a9f5-94757a43b7f5	9d6fc8ca-eef0-496e-b846-cdadac782062	⭐ (1)	0
01ca4881-1b65-4902-a1a8-b00c6330a49a	9d6fc8ca-eef0-496e-b846-cdadac782062	⭐⭐ (2)	1
c4fa5d77-d99c-40d5-90c0-343d28f91c2a	9d6fc8ca-eef0-496e-b846-cdadac782062	⭐⭐⭐ (3)	2
0156e436-5f05-435e-b44d-51b5fd38049b	9d6fc8ca-eef0-496e-b846-cdadac782062	⭐⭐⭐⭐ (4)	3
830d9633-2d53-41ad-b690-e70b96fec620	9d6fc8ca-eef0-496e-b846-cdadac782062	⭐⭐⭐⭐⭐ (5)	4
\.


--
-- TOC entry 3467 (class 0 OID 65829)
-- Dependencies: 221
-- Data for Name: polls; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.polls (id, title, question, type, status, multiple_answers, show_results, created_at, closed_at, closed_by_admin_id, organizer_fingerprint_id) FROM stdin;
9d6fc8ca-eef0-496e-b846-cdadac782062	Оцініть наш сервіс (1-5)	Виберіть оцінку від 1 до 5	RATING	ACTIVE	f	t	2025-12-22 09:51:59.292088	\N	\N	e0595da5-702b-4fef-b338-18de3ec97331
\.


--
-- TOC entry 3469 (class 0 OID 65873)
-- Dependencies: 223
-- Data for Name: votes; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.votes (id, poll_id, option_id, fingerprint_id, voted_at, text_answer) FROM stdin;
c364b5dd-0e69-48a4-8881-c4211700d3d4	9d6fc8ca-eef0-496e-b846-cdadac782062	c4fa5d77-d99c-40d5-90c0-343d28f91c2a	bb49bd02-24e8-4403-bc30-bde38261f8d5	2025-12-22 09:53:28.855889	\N
6696053b-35bf-48b0-b1ad-a18f2817c177	9d6fc8ca-eef0-496e-b846-cdadac782062	0156e436-5f05-435e-b44d-51b5fd38049b	80d27290-1e1f-4bc4-8638-1b1042ec1312	2025-12-22 09:53:38.198489	\N
7561303b-a61c-431f-9fb6-1c776104077f	9d6fc8ca-eef0-496e-b846-cdadac782062	830d9633-2d53-41ad-b690-e70b96fec620	e0595da5-702b-4fef-b338-18de3ec97331	2025-12-22 09:53:47.740851	\N
ee7ea50f-a13a-464d-b3aa-070e4282e9be	9d6fc8ca-eef0-496e-b846-cdadac782062	830d9633-2d53-41ad-b690-e70b96fec620	77e8acfc-0cea-4d21-b6c0-4cf50e635d4c	2025-12-22 09:58:33.363516	\N
\.


--
-- TOC entry 3307 (class 2606 OID 65911)
-- Name: admin_logs admin_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.admin_logs
    ADD CONSTRAINT admin_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 3279 (class 2606 OID 65806)
-- Name: admins admins_email_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_email_key UNIQUE (email);


--
-- TOC entry 3281 (class 2606 OID 65804)
-- Name: admins admins_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (id);


--
-- TOC entry 3284 (class 2606 OID 65820)
-- Name: device_fingerprints device_fingerprints_fingerprint_hash_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.device_fingerprints
    ADD CONSTRAINT device_fingerprints_fingerprint_hash_key UNIQUE (fingerprint_hash);


--
-- TOC entry 3286 (class 2606 OID 65818)
-- Name: device_fingerprints device_fingerprints_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.device_fingerprints
    ADD CONSTRAINT device_fingerprints_pkey PRIMARY KEY (id);


--
-- TOC entry 3298 (class 2606 OID 65866)
-- Name: poll_options poll_options_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.poll_options
    ADD CONSTRAINT poll_options_pkey PRIMARY KEY (id);


--
-- TOC entry 3295 (class 2606 OID 65842)
-- Name: polls polls_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.polls
    ADD CONSTRAINT polls_pkey PRIMARY KEY (id);


--
-- TOC entry 3303 (class 2606 OID 65884)
-- Name: votes uq_one_vote_per_device; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT uq_one_vote_per_device UNIQUE (poll_id, fingerprint_id);


--
-- TOC entry 3305 (class 2606 OID 65882)
-- Name: votes votes_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_pkey PRIMARY KEY (id);


--
-- TOC entry 3282 (class 1259 OID 65807)
-- Name: idx_admins_email; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_admins_email ON public.admins USING btree (email);


--
-- TOC entry 3287 (class 1259 OID 65827)
-- Name: idx_fingerprints_blocked; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_fingerprints_blocked ON public.device_fingerprints USING btree (is_blocked);


--
-- TOC entry 3288 (class 1259 OID 65828)
-- Name: idx_fingerprints_blocked_by; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_fingerprints_blocked_by ON public.device_fingerprints USING btree (blocked_by_admin_id);


--
-- TOC entry 3289 (class 1259 OID 65826)
-- Name: idx_fingerprints_hash; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_fingerprints_hash ON public.device_fingerprints USING btree (fingerprint_hash);


--
-- TOC entry 3308 (class 1259 OID 65918)
-- Name: idx_logs_action; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_logs_action ON public.admin_logs USING btree (action);


--
-- TOC entry 3309 (class 1259 OID 65917)
-- Name: idx_logs_admin; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_logs_admin ON public.admin_logs USING btree (admin_id);


--
-- TOC entry 3310 (class 1259 OID 65919)
-- Name: idx_logs_created; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_logs_created ON public.admin_logs USING btree (created_at);


--
-- TOC entry 3296 (class 1259 OID 65872)
-- Name: idx_options_poll; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_options_poll ON public.poll_options USING btree (poll_id);


--
-- TOC entry 3290 (class 1259 OID 65855)
-- Name: idx_polls_closed_by; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_polls_closed_by ON public.polls USING btree (closed_by_admin_id);


--
-- TOC entry 3291 (class 1259 OID 65856)
-- Name: idx_polls_created; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_polls_created ON public.polls USING btree (created_at);


--
-- TOC entry 3292 (class 1259 OID 65854)
-- Name: idx_polls_organizer_fingerprint; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_polls_organizer_fingerprint ON public.polls USING btree (organizer_fingerprint_id);


--
-- TOC entry 3293 (class 1259 OID 65853)
-- Name: idx_polls_status; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_polls_status ON public.polls USING btree (status);


--
-- TOC entry 3299 (class 1259 OID 65901)
-- Name: idx_votes_fingerprint; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_votes_fingerprint ON public.votes USING btree (fingerprint_id);


--
-- TOC entry 3300 (class 1259 OID 65902)
-- Name: idx_votes_option; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_votes_option ON public.votes USING btree (option_id);


--
-- TOC entry 3301 (class 1259 OID 65900)
-- Name: idx_votes_poll; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_votes_poll ON public.votes USING btree (poll_id);


--
-- TOC entry 3319 (class 2620 OID 65921)
-- Name: votes trg_update_device_on_vote; Type: TRIGGER; Schema: public; Owner: neondb_owner
--

CREATE TRIGGER trg_update_device_on_vote AFTER INSERT ON public.votes FOR EACH ROW EXECUTE FUNCTION public.update_device_last_seen();


--
-- TOC entry 3318 (class 2606 OID 65912)
-- Name: admin_logs admin_logs_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.admin_logs
    ADD CONSTRAINT admin_logs_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES public.admins(id) ON DELETE SET NULL;


--
-- TOC entry 3311 (class 2606 OID 65821)
-- Name: device_fingerprints device_fingerprints_blocked_by_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.device_fingerprints
    ADD CONSTRAINT device_fingerprints_blocked_by_admin_id_fkey FOREIGN KEY (blocked_by_admin_id) REFERENCES public.admins(id) ON DELETE SET NULL;


--
-- TOC entry 3314 (class 2606 OID 65867)
-- Name: poll_options poll_options_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.poll_options
    ADD CONSTRAINT poll_options_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.polls(id) ON DELETE CASCADE;


--
-- TOC entry 3312 (class 2606 OID 65843)
-- Name: polls polls_closed_by_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.polls
    ADD CONSTRAINT polls_closed_by_admin_id_fkey FOREIGN KEY (closed_by_admin_id) REFERENCES public.admins(id) ON DELETE SET NULL;


--
-- TOC entry 3313 (class 2606 OID 65848)
-- Name: polls polls_organizer_fingerprint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.polls
    ADD CONSTRAINT polls_organizer_fingerprint_id_fkey FOREIGN KEY (organizer_fingerprint_id) REFERENCES public.device_fingerprints(id) ON DELETE CASCADE;


--
-- TOC entry 3315 (class 2606 OID 65895)
-- Name: votes votes_fingerprint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_fingerprint_id_fkey FOREIGN KEY (fingerprint_id) REFERENCES public.device_fingerprints(id);


--
-- TOC entry 3316 (class 2606 OID 65890)
-- Name: votes votes_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.poll_options(id) ON DELETE SET NULL;


--
-- TOC entry 3317 (class 2606 OID 65885)
-- Name: votes votes_poll_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES public.polls(id) ON DELETE CASCADE;


--
-- TOC entry 2113 (class 826 OID 16394)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: cloud_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE cloud_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO neon_superuser WITH GRANT OPTION;


--
-- TOC entry 2112 (class 826 OID 16393)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: cloud_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE cloud_admin IN SCHEMA public GRANT ALL ON TABLES TO neon_superuser WITH GRANT OPTION;


-- Completed on 2025-12-22 10:01:21

--
-- PostgreSQL database dump complete
--

\unrestrict ilUCYfQDRZFOJRjHxOvnrG62hvfRFcv8sZG16UvooFH0Q8A7br0Er31yNwuJyOf

